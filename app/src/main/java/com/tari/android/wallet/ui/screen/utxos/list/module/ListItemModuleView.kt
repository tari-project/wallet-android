package com.tari.android.wallet.ui.screen.utxos.list.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleUtxosOrderingItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible

@SuppressLint("ViewConstructor")
class ListItemModuleView(context: Context, buttonModule: ListItemModule) :
    CommonView<CommonViewModel, DialogModuleUtxosOrderingItemBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleUtxosOrderingItemBinding =
        DialogModuleUtxosOrderingItemBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.item.setText(buttonModule.ordering.textId)
        buttonModule.listener = {
            ui.icon.setVisible(it)
        }
        ui.root.setOnClickListener { buttonModule.click() }
        ui.icon.setVisible(buttonModule.isSelected)
    }
}