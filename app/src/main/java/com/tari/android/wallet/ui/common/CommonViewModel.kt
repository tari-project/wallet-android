package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import io.reactivex.disposables.CompositeDisposable

open class CommonViewModel : ViewModel() {

    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    internal val component: ApplicationComponent?
        get() = TariWalletApplication.INSTANCE.get()?.appComponent

    init {
        component?.inject(this)
    }

    override fun onCleared() {
        super.onCleared()

        compositeDisposable.clear()

        //todo double check it
        EventBus.unsubscribe(this)
        EventBus.torProxyState.unsubscribe(this)
        EventBus.walletState.unsubscribe(this)
        EventBus.networkConnectionState.unsubscribe(this)
        EventBus.backupState.unsubscribe(this)
        EventBus.baseNodeState.unsubscribe(this)
    }

    protected val _openLink = SingleLiveEvent<String>()
    val openLink: LiveData<String> = _openLink

    protected val _confirmDialog = SingleLiveEvent<ConfirmDialogArgs>()
    val confirmDialog: LiveData<ConfirmDialogArgs> = _confirmDialog
}