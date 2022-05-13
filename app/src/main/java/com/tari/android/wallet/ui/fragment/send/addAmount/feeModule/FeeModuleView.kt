package com.tari.android.wallet.ui.fragment.send.addAmount.feeModule

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleFeeBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.fragment.send.amountView.AmountStyle

@SuppressLint("ViewConstructor")
class FeeModuleView(context: Context, private val feeModule: FeeModule) :
    CommonView<CommonViewModel, DialogModuleFeeBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleFeeBinding =
        DialogModuleFeeBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    var networkSpeed: NetworkSpeed = NetworkSpeed.Medium

    init {
        ui.amount.setupArgs(context.dimenPx(R.dimen.add_amount_custom_fee_size).toFloat())
        ui.amount.setupArgs(feeModule.fee)
        ui.amount.setupArgs(AmountStyle.Normal)
        ui.networkSlow.setOnClickListener { applySpeed(NetworkSpeed.Slow) }
        ui.networkMedium.setOnClickListener { applySpeed(NetworkSpeed.Medium) }
        ui.networkFast.setOnClickListener { applySpeed(NetworkSpeed.Fast) }
        applySpeed(feeModule.networkSpeed)
    }

    private fun applySpeed(speed: NetworkSpeed) {
        this.networkSpeed = speed
        val views = listOf(ui.networkMedium, ui.networkFast, ui.networkSlow)
        views.forEach { it.setColorFilter(ContextCompat.getColor(context, R.color.black)) }

        val currentSpeed = when(networkSpeed) {
            NetworkSpeed.Fast -> ui.networkFast
            NetworkSpeed.Medium -> ui.networkMedium
            NetworkSpeed.Slow -> ui.networkSlow
        }
        currentSpeed.setColorFilter(ContextCompat.getColor(context, R.color.purple))
    }
}