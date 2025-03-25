package com.tari.android.wallet.ui.common

import android.app.Activity
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.ConnectionStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.permission.PermissionManager
import com.tari.android.wallet.model.CoreError
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.Navigation.AllSettings
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModule
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.connection.ConnectionStatusesModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.util.extension.addTo
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import io.reactivex.disposables.CompositeDisposable
import yat.android.lib.YatIntegration
import javax.inject.Inject

open class CommonViewModel : ViewModel(), DialogHandler {

    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    val component: ApplicationComponent
        get() = DiContainer.appComponent

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var networkRepository: NetworkPrefRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsPrefRepository

    @Inject
    lateinit var tariNavigator: TariNavigator

    @Inject
    lateinit var sharedPrefsRepository: CorePrefRepository

    @Inject
    lateinit var securityPrefRepository: SecurityPrefRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var connectionStateHandler: ConnectionStateHandler

    @Inject
    lateinit var dialogManager: DialogManager

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    init {
        component.inject(this) // This injects the dependencies to the base class
    }

    private var authorizedAction: (() -> Unit)? = null

    val logger: Printer
        get() = Logger.t(this::class.simpleName)

    val currentTheme = tariSettingsSharedRepository.currentTheme

    val connectionState = connectionStateHandler.connectionState

    private val _backPressed = SingleLiveEvent<Unit>()
    val backPressed: LiveData<Unit> = _backPressed

    protected val _openLink = SingleLiveEvent<String>()
    val openLink: LiveData<String> = _openLink

    protected val _showToast = SingleLiveEvent<TariToastArgs>()
    val showToast: LiveData<TariToastArgs> = _showToast

    private val _copyToClipboard = SingleLiveEvent<ClipboardArgs>()
    val copyToClipboard: LiveData<ClipboardArgs> = _copyToClipboard

    private val _modularDialog = SingleLiveEvent<ModularDialogArgs>()
    val modularDialog: LiveData<ModularDialogArgs> = _modularDialog

    private val _inputDialog = SingleLiveEvent<ModularDialogArgs>()
    val inputDialog: LiveData<ModularDialogArgs> = _inputDialog

    protected val _blockedBackPressed = SingleLiveEvent<Boolean>()
    val blockedBackPressed: LiveData<Boolean> = _blockedBackPressed

    init {
        securityPrefRepository.updateNotifier.subscribe {
            checkAuthorization()
        }.addTo(compositeDisposable)

        collectFlow(connectionState) { connectionState ->
            showConnectionStatusDialog(refresh = true)
        }
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable.clear()
    }

    fun doOnWalletRunning(action: suspend (walletService: FFIWallet) -> Unit) {
        launchOnIo {
            walletManager.doOnWalletRunning(action)
        }
    }

    fun runWithAuthorization(action: () -> Unit) {
        authorizedAction = action
        tariNavigator.navigate(Navigation.Auth.FeatureAuth)
    }

    private fun checkAuthorization() {
        if (authorizedAction != null && securityPrefRepository.isFeatureAuthenticated) {
            securityPrefRepository.isFeatureAuthenticated = false
            onBackPressed()
            authorizedAction?.invoke()
            authorizedAction = null
        }
    }

    fun processIntentDeepLink(activity: Activity, intent: Intent) {
        intent.data?.let { deeplinkUri ->
            YatIntegration.processDeepLink(activity, deeplinkUri)
            deeplinkManager.parseDeepLink(deeplinkUri)
        }?.let { deeplink ->
            deeplinkManager.execute(activity, deeplink)
        }
    }

    fun onBackPressed() {
        _backPressed.value = Unit
    }

    override fun showModularDialog(args: ModularDialogArgs) {
        _modularDialog.postValue(args)
    }

    override fun showModularDialog(vararg modules: IDialogModule) {
        _modularDialog.postValue(ModularDialogArgs(modules = modules.toList()))
    }

    override fun showModularDialog(dialogId: Int, vararg modules: IDialogModule) {
        _modularDialog.postValue(ModularDialogArgs(dialogId = dialogId, modules = modules.toList()))
    }

    override fun showInputModalDialog(inputArgs: ModularDialogArgs) {
        _inputDialog.postValue(inputArgs)
    }

    override fun showInputModalDialog(vararg modules: IDialogModule) {
        _inputDialog.postValue(ModularDialogArgs(modules = modules.toList()))
    }

    override fun showErrorDialog(error: CoreError) {
        showModularDialog(WalletErrorArgs(resourceManager, error).getModular())
    }

    override fun showErrorDialog(exception: Throwable, onClose: () -> Unit) {
        showModularDialog(WalletErrorArgs(resourceManager, exception, onClose).getModular())
    }

    override fun showNotReadyYetDialog() {
        showSimpleDialog(
            iconRes = R.drawable.tari_construction,
            title = resourceManager.getString(R.string.common_not_ready_yet_dialog_title),
            description = resourceManager.getString(R.string.common_not_ready_yet_dialog_description),
        )
    }

    override fun showSimpleDialog(
        @DrawableRes iconRes: Int?,
        @StringRes titleRes: Int,
        @StringRes descriptionRes: Int,
        cancelable: Boolean,
        canceledOnTouchOutside: Boolean,
        @StringRes closeButtonTextRes: Int,
        onClose: () -> Unit,
    ) {
        showSimpleDialog(
            iconRes = iconRes,
            title = resourceManager.getString(titleRes),
            description = resourceManager.getString(descriptionRes),
            cancelable = cancelable,
            canceledOnTouchOutside = canceledOnTouchOutside,
            closeButtonTextRes = closeButtonTextRes,
            onClose = onClose,
        )
    }

    override fun showSimpleDialog(
        @DrawableRes iconRes: Int?,
        title: CharSequence,
        description: CharSequence,
        cancelable: Boolean,
        canceledOnTouchOutside: Boolean,
        @StringRes closeButtonTextRes: Int,
        onClose: () -> Unit,
    ) {
        showModularDialog(
            SimpleDialogArgs(iconRes, title, description, cancelable, canceledOnTouchOutside, closeButtonTextRes, onClose)
                .getModular(resourceManager)
        )
    }

    override fun showAddressDetailsDialog(walletAddress: TariWalletAddress) {
        showModularDialog(
            HeadModule(
                title = resourceManager.getString(R.string.wallet_info_address_details_title),
                rightButtonIcon = R.drawable.vector_common_close,
                rightButtonAction = { hideDialog() },
            ),
            AddressDetailsModule(
                tariWalletAddress = walletAddress,
                copyBase58 = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullBase58,
                    )
                },
                copyEmojis = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullEmojiId,
                    )
                },
            )
        )
    }

    override fun showInternetConnectionErrorDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.internet_connection_error_dialog_title),
            description = resourceManager.getString(R.string.internet_connection_error_dialog_description),
        )
    }

    override fun showWalletErrorDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.common_error_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_details_connected_wallets_no_application)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
        )
    }


    override fun showConnectionStatusDialog(refresh: Boolean) {
        if (!refresh || dialogManager.isDialogShowing(ModularDialogArgs.DialogId.CONNECTION_STATUS)) {
            showModularDialog(
                ModularDialogArgs(
                    dialogId = ModularDialogArgs.DialogId.CONNECTION_STATUS,
                    modules = listOf(
                        HeadModule(resourceManager.getString(R.string.connection_status_dialog_title)),
                        ConnectionStatusesModule(connectionState.value),
                        ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                    ),
                )
            )
        }
    }

    override fun hideDialog(dialogId: Int) {
        launchOnMain {
            dialogManager.dismiss(dialogId)
        }
    }

    override fun hideDialogImmediately(dialogId: Int) {
        dialogManager.dismiss(dialogId, true)
    }

    internal fun onScreenCaptured() {
        if (!tariSettingsSharedRepository.screenRecordingTurnedOn) {
            showModularDialog(
                ConfirmDialogArgs(
                    dialogId = ModularDialogArgs.DialogId.SCREEN_RECORDING,
                    title = resourceManager.getString(R.string.screen_recording_disabled_dialog_title),
                    description = resourceManager.getString(R.string.screen_recording_disabled_dialog_description),
                    confirmButtonText = resourceManager.getString(R.string.screen_recording_disabled_dialog_confirm_button),
                    cancelButtonText = resourceManager.getString(R.string.screen_recording_disabled_dialog_cancel_button),
                    onConfirm = { hideDialog(ModularDialogArgs.DialogId.SCREEN_RECORDING) },
                    onCancel = { tariNavigator.navigate(AllSettings.ToScreenRecording) },
                    onDismiss = { },
                ).getModular(resourceManager)
            )
        }
    }

    internal fun copyToClipboard(
        clipLabel: String,
        clipText: String,
        toastMessage: String = resourceManager.getString(R.string.common_copied_to_clipboard)
    ) {
        _copyToClipboard.postValue(ClipboardArgs(clipLabel, clipText, toastMessage))
    }
}

interface DialogHandler {
    fun showModularDialog(vararg modules: IDialogModule)
    fun showModularDialog(dialogId: Int = ModularDialogArgs.DialogId.NO_ID, vararg modules: IDialogModule)
    fun showModularDialog(args: ModularDialogArgs)
    fun showSimpleDialog(
        iconRes: Int? = null,
        title: CharSequence,
        description: CharSequence,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        closeButtonTextRes: Int = R.string.common_close,
        onClose: () -> Unit = {}
    )

    fun showSimpleDialog(
        iconRes: Int? = null,
        titleRes: Int,
        descriptionRes: Int,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        closeButtonTextRes: Int = R.string.common_close,
        onClose: () -> Unit = {}
    )

    fun showErrorDialog(exception: Throwable, onClose: () -> Unit = {})
    fun showErrorDialog(error: CoreError)
    fun showInputModalDialog(vararg modules: IDialogModule)
    fun showInputModalDialog(inputArgs: ModularDialogArgs)
    fun hideDialog(dialogId: Int = ModularDialogArgs.DialogId.NO_ID)
    fun hideDialogImmediately(dialogId: Int = ModularDialogArgs.DialogId.NO_ID)

    fun showNotReadyYetDialog()
    fun showAddressDetailsDialog(walletAddress: TariWalletAddress)
    fun showInternetConnectionErrorDialog()
    fun showWalletErrorDialog()
    fun showConnectionStatusDialog(refresh: Boolean = false)
}