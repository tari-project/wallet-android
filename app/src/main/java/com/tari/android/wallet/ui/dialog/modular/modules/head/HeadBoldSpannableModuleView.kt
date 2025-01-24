package com.tari.android.wallet.ui.dialog.modular.modules.head

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleHeadBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.util.extension.applyFontStyle
import com.tari.android.wallet.util.extension.string

@SuppressLint("ViewConstructor")
class HeadBoldSpannableModuleView(context: Context, buttonModule: HeadBoldSpannableModule) :
    CommonView<CommonViewModel, DialogModuleHeadBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleHeadBinding =
        DialogModuleHeadBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        val title = context.string(buttonModule.regularTitlePart).applyFontStyle(
            context = context,
            defaultFont = TariFont.LIGHT,
            search = listOf(context.string(buttonModule.boldTitlePart)),
            tariFont = TariFont.BLACK,
        )

        ui.head.text = title
    }
}