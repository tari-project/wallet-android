package com.tari.android.wallet.ui.screen.utxos.list.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.databinding.DialogModuleUtxoSplitBinding
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import java.math.BigInteger

@SuppressLint("ViewConstructor")
class UtxoSplitModuleView(context: Context, val buttonModule: UtxoSplitModule) :
    CommonView<CommonViewModel, DialogModuleUtxoSplitBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleUtxoSplitBinding =
        DialogModuleUtxoSplitBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateValue(progress)
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        ui.plusButton.setOnClickListener { ui.seekbar.progress = ui.seekbar.progress + 1 }
        ui.minusButton.setOnClickListener { ui.seekbar.progress = ui.seekbar.progress - 1 }
        ui.seekbar.min = seekBarMin
        ui.seekbar.max = seekBarMax
        ui.seekbar.progress = seekBarDefault
    }

    private fun updateValue(newValue: Int) {
        ui.amount.text = newValue.toString()
        buttonModule.count = newValue
        val previewValues = buttonModule.previewMaker(newValue, buttonModule.items)
        val fullAmountBigInteger = buttonModule.items.map { it.value.tariValue }.sumOf { it }
        val amountText = WalletConfig.amountFormatter.format(fullAmountBigInteger)!!
        val intoAmount = context.getString(R.string.utxos_split_preview_into) + " " + newValue + " x"
        val feeAmount = WalletConfig.amountFormatter.format(previewValues.feeValue.tariValue)!!
        val mostBestAmount = MicroTari(BigInteger.valueOf(previewValues.vector.longs[0]))
        val coinsValues =
            WalletConfig.amountFormatter.format(mostBestAmount.tariValue)!! + " " + context.getString(R.string.utxos_split_preview_coins)
        ui.fullAmount.text = amountText
        ui.intoAmount.text = intoAmount
        ui.feeAmount.text = feeAmount
        ui.countCoins.text = coinsValues
    }

    companion object {
        const val seekBarMin = 2
        const val seekBarMax = 50
        const val seekBarDefault = 2
    }
}