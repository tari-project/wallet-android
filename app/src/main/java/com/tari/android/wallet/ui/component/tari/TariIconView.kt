package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.tari.android.wallet.ui.common.domain.PaletteManager

class TariIconView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    init {
        imageTintList = ColorStateList.valueOf(PaletteManager.getIconDefault(context))
    }
}