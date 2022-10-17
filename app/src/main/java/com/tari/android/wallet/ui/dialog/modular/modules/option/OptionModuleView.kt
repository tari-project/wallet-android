package com.tari.android.wallet.ui.dialog.modular.modules.option

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleOptionBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class OptionModuleView(context: Context, buttonModule: OptionModule) : CommonView<CommonViewModel, DialogModuleOptionBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleOptionBinding =
        DialogModuleOptionBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.body.text = buttonModule.text
        ui.root.setOnClickListener { buttonModule.action.invoke() }
    }
}