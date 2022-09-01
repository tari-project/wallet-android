package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.dialog.inProgress.ProgressDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

open class CommonViewModel : ViewModel() {

    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    val component: ApplicationComponent
        get() = DiContainer.appComponent


    @Inject
    lateinit var resourceManager: ResourceManager

    val logger = Logger.t("screen").t(this::class.simpleName)

    init {
        @Suppress("LeakingThis")
        component.inject(this)

        logger.i(this::class.simpleName + "was started")

        EventBus.walletState.publishSubject.filter { it is WalletState.Failed }
            .subscribe {
                val errorArgs = WalletErrorArgs(resourceManager, (it as WalletState.Failed).exception).getErrorArgs().getModular(resourceManager)
                _modularDialog.postValue(errorArgs)
            }.addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable.clear()

        EventBus.unsubscribeAll(this)
    }

    protected val _backPressed = SingleLiveEvent<Unit>()
    val backPressed: LiveData<Unit> = _backPressed

    protected val _openLink = SingleLiveEvent<String>()
    val openLink: LiveData<String> = _openLink

    protected val _copyToClipboard = SingleLiveEvent<ClipboardArgs>()
    val copyToClipboard: LiveData<ClipboardArgs> = _copyToClipboard

    protected val _modularDialog = SingleLiveEvent<ModularDialogArgs>()
    val modularDialog: LiveData<ModularDialogArgs> = _modularDialog

    protected val _loadingDialog = SingleLiveEvent<ProgressDialogArgs>()
    val loadingDialog: LiveData<ProgressDialogArgs> = _loadingDialog

    protected val _dismissDialog = SingleLiveEvent<Unit>()
    val dismissDialog: LiveData<Unit> = _dismissDialog

    protected val _blockedBackPressed = SingleLiveEvent<Boolean>()
    val blockedBackPressed: LiveData<Boolean> = _blockedBackPressed
}