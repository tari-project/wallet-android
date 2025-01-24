package com.tari.android.wallet.ui.dialog.modular.modules.head

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleHeadBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.ui.component.tari.TariTypefaceSpan

@SuppressLint("ViewConstructor")
class HeadSpannableModuleView(context: Context, buttonModule: HeadSpannableModule) : CommonView<CommonViewModel, DialogModuleHeadBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleHeadBinding =
        DialogModuleHeadBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        val title = SpannableStringBuilder().apply {
            val highlightedPart = SpannableString(buttonModule.highlightedTitlePart)
            highlightedPart.setSpan(
                /* what = */ TariTypefaceSpan("", TariFont.HEAVY.asTypeface(context)),
                /* start = */ 0,
                /* end = */ highlightedPart.length,
                /* flags = */ Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            insert(0, buttonModule.regularTitlePart)
            insert(buttonModule.regularTitlePart.length, " ")
            insert(buttonModule.regularTitlePart.length + 1, highlightedPart)
        }

        ui.head.text = title
    }
}