package com.tari.android.wallet.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.di.ApplicationComponent

open class CommonViewModel : ViewModel() {

    internal val component: ApplicationComponent?
        get() = TariWalletApplication.INSTANCE.get()?.appComponent

    init {
        component?.inject(this)
    }

    private val _openLink = MutableLiveData<String>()
    val openLink: LiveData<String> = _openLink
}