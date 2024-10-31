package com.tari.android.wallet.ui.fragment.utxos.list.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.databinding.DialogModuleUtxoAmountBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class UtxoAmountModuleView(context: Context, buttonModule: UtxoAmountModule) :
    CommonView<CommonViewModel, DialogModuleUtxoAmountBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleUtxoAmountBinding =
        DialogModuleUtxoAmountBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.amount.text = WalletConfig.amountFormatter.format(buttonModule.amount.tariValue)!!
    }
}