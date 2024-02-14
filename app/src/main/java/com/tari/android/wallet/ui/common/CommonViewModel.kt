package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.ServiceConnectionStatus
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.permission.PermissionManager
import com.tari.android.wallet.ui.component.tari.toast.TariToastArgs
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.dialog.inProgress.ProgressDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

open class CommonViewModel : ViewModel() {

    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    val component: ApplicationComponent
        get() = DiContainer.appComponent

    val serviceConnection: TariWalletServiceConnection = TariWalletServiceConnection()
    val walletService: TariWalletService
        get() = serviceConnection.currentState.service!!

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    @Inject
    lateinit var paletteManager: PaletteManager

    @Inject
    lateinit var tariNavigator: TariNavigator

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    @Inject
    lateinit var securityPrefRepository: SecurityPrefRepository

    private var authorizedAction: (() -> Unit)? = null

    val logger: Printer
        get() = Logger.t(this::class.simpleName).t(LoggerTags.UI.name)

    val currentTheme = SingleLiveEvent<TariTheme>()

    val backPressed = SingleLiveEvent<Unit>()

    protected val _openLink = SingleLiveEvent<String>()
    val openLink: LiveData<String> = _openLink

    protected val _showToast = SingleLiveEvent<TariToastArgs>()
    val showToast: LiveData<TariToastArgs> = _showToast

    protected val _copyToClipboard = SingleLiveEvent<ClipboardArgs>()
    val copyToClipboard: LiveData<ClipboardArgs> = _copyToClipboard

    val modularDialog = SingleLiveEvent<ModularDialogArgs>()

    protected val _inputDialog = SingleLiveEvent<ModularDialogArgs>()
    val inputDialog: LiveData<ModularDialogArgs> = _inputDialog

    protected val _loadingDialog = SingleLiveEvent<ProgressDialogArgs>()
    val loadingDialog: LiveData<ProgressDialogArgs> = _loadingDialog

    val dismissDialog: SingleLiveEvent<Unit> = SingleLiveEvent()

    protected val _blockedBackPressed = SingleLiveEvent<Boolean>()
    val blockedBackPressed: LiveData<Boolean> = _blockedBackPressed

    val navigation: SingleLiveEvent<Navigation> = SingleLiveEvent()

    init {
        @Suppress("LeakingThis")
        component.inject(this)

        currentTheme.value = tariSettingsSharedRepository.currentTheme!!

        logger.t(LoggerTags.Navigation.name).i(this::class.simpleName + " was started")

        securityPrefRepository.updateNotifier.subscribe {
            checkAuthorization()
        }.addTo(compositeDisposable)

        EventBus.walletState.publishSubject.filter { it is WalletState.Failed }
            .subscribe({
                val exception = (it as WalletState.Failed).exception
                val errorArgs = WalletErrorArgs(resourceManager, exception).getErrorArgs().getModular(resourceManager, true)
                modularDialog.postValue(errorArgs)
            }, {
                logger.i(it.toString())
                logger.i("on showing error dialog from wallet")
            })
            .addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable.clear()

        EventBus.unsubscribeAll(this)
    }

    fun doOnConnected(action: (walletService: TariWalletService) -> Unit) {
        serviceConnection.connection.filter { it.status == ServiceConnectionStatus.CONNECTED }.take(1)
            .doOnError { logger.i(it.toString()) }
            .subscribe { action(it.service!!) }
            .addTo(compositeDisposable)
    }

    fun doOnConnectedToWallet(action: (walletService: FFIWallet) -> Unit) {
        EventBus.walletState.publishSubject.filter { it == WalletState.Running }.take(1).doOnError { Logger.i(it.toString()) }
            .subscribe { action(FFIWallet.instance!!) }
            .addTo(compositeDisposable)
    }

    fun openWalletErrorDialog() {
        val modularArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.common_error_title)),
                BodyModule(resourceManager.getString(R.string.contact_book_details_connected_wallets_no_application)),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(modularArgs)
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
}