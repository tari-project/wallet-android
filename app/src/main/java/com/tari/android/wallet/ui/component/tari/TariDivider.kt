package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.dpToPx

class TariDivider(context: Context, attrs: AttributeSet) : View(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_neutral_secondary)
        setBackgroundColor(backColor)
    }
}