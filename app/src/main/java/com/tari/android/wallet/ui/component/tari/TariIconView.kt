package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute

class TariIconView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    init {
        val tintColor = context.colorFromAttribute(R.attr.palette_icons_default)
        imageTintList = ColorStateList.valueOf(tintColor)
    }
}