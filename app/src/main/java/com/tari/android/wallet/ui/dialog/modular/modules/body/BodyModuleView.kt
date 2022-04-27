package com.tari.android.wallet.ui.dialog.modular.modules.body

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleBodyBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class BodyModuleView(context: Context, buttonModule: BodyModule) : CommonView<CommonViewModel, DialogModuleBodyBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleBodyBinding =
        DialogModuleBodyBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.body.text = buttonModule.text ?: buttonModule.textSpannable
    }
}