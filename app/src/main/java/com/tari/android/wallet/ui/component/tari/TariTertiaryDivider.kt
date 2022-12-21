package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute

class TariTertiaryDivider(context: Context, attrs: AttributeSet) : View(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_neutral_tertiary)
        setBackgroundColor(backColor)
    }
}