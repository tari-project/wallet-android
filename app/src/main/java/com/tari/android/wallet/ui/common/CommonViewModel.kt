package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs

open class CommonViewModel : ViewModel() {

    internal val component: ApplicationComponent?
        get() = TariWalletApplication.INSTANCE.get()?.appComponent

    init {
        component?.inject(this)
    }

    protected val _openLink = SingleLiveEvent<String>()
    val openLink: LiveData<String> = _openLink

    protected val _confirmDialog = SingleLiveEvent<ConfirmDialogArgs>()
    val confirmDialog: LiveData<ConfirmDialogArgs> = _confirmDialog
}