package com.tari.android.wallet.ui.dialog.modular.modules.head

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleHeadBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.setVisible

@SuppressLint("ViewConstructor")
class HeadModuleView(context: Context, buttonModule: HeadModule) : CommonView<CommonViewModel, DialogModuleHeadBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleHeadBinding =
        DialogModuleHeadBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.head.text = buttonModule.title
        ui.button.ui.button.text = buttonModule.rightButtonTitle
        ui.button.setOnClickListener { buttonModule.rightButtonAction() }
        ui.button.setVisible(buttonModule.rightButtonTitle.isNotEmpty())
    }
}