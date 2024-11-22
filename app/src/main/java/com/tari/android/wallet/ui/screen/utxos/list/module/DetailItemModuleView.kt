package com.tari.android.wallet.ui.screen.utxos.list.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.tari.android.wallet.databinding.DialogModuleUtxosDetailItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class DetailItemModuleView(context: Context, buttonModule: DetailItemModule) :
    CommonView<CommonViewModel, DialogModuleUtxosDetailItemBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleUtxosDetailItemBinding =
        DialogModuleUtxosDetailItemBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.detailTitle.text = buttonModule.title
        ui.detailDescription.text = buttonModule.description

        val drawable = buttonModule.descriptionIcon?.let { ContextCompat.getDrawable(context, it) }
        ui.detailDescription.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }
}