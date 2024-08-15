package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import com.tari.android.wallet.model.CoreError
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.permission.PermissionManager
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
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.AllSettingsNavigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

open class CommonViewModel : ViewModel() {

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
    lateinit var serviceConnection: TariWalletServiceConnection
    val walletService: TariWalletService
        get() = serviceConnection.walletService

    @Inject
    lateinit var walletStateHandler: WalletStateHandler

    @Inject
    lateinit var dialogManager: DialogManager

    private var authorizedAction: (() -> Unit)? = null

    val logger: Printer
        get() = Logger.t(this::class.simpleName)

    val currentTheme = SingleLiveEvent<TariTheme>()

    val backPressed = SingleLiveEvent<Unit>()

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

    // TODO don't use it. Use tariNavigator.navigate() instead
    val navigation: SingleLiveEvent<Navigation> = SingleLiveEvent()

    init {
        component.inject(this)

        currentTheme.value = tariSettingsSharedRepository.currentTheme

        logger.t(LoggerTags.Navigation.name).i(this::class.simpleName + " was started")

        securityPrefRepository.updateNotifier.subscribe {
            checkAuthorization()
        }.addTo(compositeDisposable)

        viewModelScope.launch {
            walletStateHandler.doOnWalletFailed {
                showErrorDialog(it)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable.clear()

        EventBus.unsubscribeAll(this)
    }

    fun doOnWalletServiceConnected(action: suspend (walletService: TariWalletService) -> Unit) {
        viewModelScope.launch {
            serviceConnection.doOnWalletServiceConnected(action)
        }
    }

    fun doOnWalletRunning(action: suspend (walletService: FFIWallet) -> Unit) {
        viewModelScope.launch {
            walletStateHandler.doOnWalletRunning(action)
        }
    }

    fun openWalletErrorDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.common_error_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_details_connected_wallets_no_application)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
        )
    }

    fun runWithAuthorization(action: () -> Unit) {
        authorizedAction = action
        navigation.postValue(Navigation.FeatureAuth)
    }

    fun checkAuthorization() {
        if (authorizedAction != null && securityPrefRepository.isFeatureAuthenticated) {
            securityPrefRepository.isFeatureAuthenticated = false
            backPressed.value = Unit
            authorizedAction?.invoke()
            authorizedAction = null
        }
    }

    fun doOnBackground(action: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch { action() }

    fun showModularDialog(args: ModularDialogArgs) {
        _modularDialog.postValue(args)
    }

    fun showModularDialog(vararg modules: IDialogModule) {
        _modularDialog.postValue(ModularDialogArgs(modules = modules.toList()))
    }

    fun showInputModalDialog(inputArgs: ModularDialogArgs) {
        _inputDialog.postValue(inputArgs)
    }

    fun showInputModalDialog(vararg modules: IDialogModule) {
        _inputDialog.postValue(ModularDialogArgs(modules = modules.toList()))
    }

    fun showErrorDialog(error: CoreError) {
        showModularDialog(WalletErrorArgs(resourceManager, error).getModular())
    }

    fun showErrorDialog(exception: Throwable) {
        showModularDialog(WalletErrorArgs(resourceManager, exception).getModular())
    }

    fun showNotReadyYetDialog() {
        showSimpleDialog(
            iconRes = R.drawable.tari_construction,
            title = resourceManager.getString(R.string.common_not_ready_yet_dialog_title),
            description = resourceManager.getString(R.string.common_not_ready_yet_dialog_description),
        )
    }

    fun showSimpleDialog(
        iconRes: Int? = null,
        title: CharSequence,
        description: CharSequence,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        closeButtonTextRes: Int = R.string.common_close,
        onClose: () -> Unit = {},
    ) {
        showModularDialog(
            SimpleDialogArgs(iconRes, title, description, cancelable, canceledOnTouchOutside, closeButtonTextRes, onClose)
                .getModular(resourceManager)
        )
    }

    protected fun showAddressDetailsDialog(walletAddress: TariWalletAddress) {
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

    fun hideDialog(dialogId: Int = ModularDialogArgs.DialogId.NO_ID) {
        viewModelScope.launch(Dispatchers.Main) {
            dialogManager.dismiss(dialogId)
        }
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
                    onCancel = { tariNavigator.navigate(AllSettingsNavigation.ToScreenRecording) },
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