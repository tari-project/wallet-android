package com.tari.android.wallet.ui.fragment.send.addAmount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.FeeModule
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.NetworkSpeed
import javax.inject.Inject

class AddAmountViewModel() : CommonViewModel() {

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    private val _isOneSidePaymentEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val isOneSidePaymentEnabled: LiveData<Boolean> = _isOneSidePaymentEnabled

    var networkSpeed = NetworkSpeed.Medium

    init {
        component.inject(this)

        _isOneSidePaymentEnabled.postValue(tariSettingsSharedRepository.isOneSidePaymentEnabled)
    }

    fun toggleOneSidePayment() {
        val newValue = !tariSettingsSharedRepository.isOneSidePaymentEnabled
        tariSettingsSharedRepository.isOneSidePaymentEnabled = newValue
    }

    fun showFeeDialog() {
        val args = ModularDialogArgs(
            DialogArgs(),
            listOf(
                HeadModule(resourceManager.getString(R.string.add_amount_modify_fee_title)),
                BodyModule(resourceManager.getString(R.string.add_amount_modify_fee_description)),
                FeeModule(MicroTari(), networkSpeed),
                ButtonModule(resourceManager.getString(R.string.add_amount_modify_fee_use), ButtonStyle.Normal),
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }
}