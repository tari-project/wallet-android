package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.setColor

class TariProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        setColor(context.colorFromAttribute(R.attr.palette_brand_purple))
    }

    fun setWhite() = setColor(context.getColor(R.color.white))

    fun setError() = setColor(context.colorFromAttribute(R.attr.palette_system_red))
}