package com.tari.android.wallet.ui.dialog.modular.modules.space

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleSpaceBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.dpToPx
import com.tari.android.wallet.util.extension.setLayoutHeight

@SuppressLint("ViewConstructor")
class SpaceModuleView(context: Context, spaceModule: SpaceModule) : CommonView<CommonViewModel, DialogModuleSpaceBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleSpaceBinding =
        DialogModuleSpaceBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.root.setLayoutHeight(context.dpToPx(spaceModule.space.toFloat()).toInt())
    }
}