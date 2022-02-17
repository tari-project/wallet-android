package com.tari.android.wallet.ui.fragment.send.addAmount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class AddAmountViewModel() : CommonViewModel() {

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    private val _isOneSidePaymentEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val isOneSidePaymentEnabled: LiveData<Boolean> = _isOneSidePaymentEnabled

    init {
        component.inject(this)

        _isOneSidePaymentEnabled.postValue(tariSettingsSharedRepository.isOneSidePaymentEnabled)
    }

    fun toggleOneSidePayment() {
        val newValue = !tariSettingsSharedRepository.isOneSidePaymentEnabled
        tariSettingsSharedRepository.isOneSidePaymentEnabled = newValue
    }
}