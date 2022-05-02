package com.tari.android.wallet.ui.dialog.modular.modules.imageModule

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleImageBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class ImageModuleView(context: Context, buttonModule: ImageModule) : CommonView<CommonViewModel, DialogModuleImageBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleImageBinding =
        DialogModuleImageBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.image.setImageResource(buttonModule.imageResource)
    }
}