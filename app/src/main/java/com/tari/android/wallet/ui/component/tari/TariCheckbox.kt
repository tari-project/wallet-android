package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.tari.android.wallet.ui.common.domain.PaletteManager

class TariCheckbox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    init {
        buttonTintList = ColorStateList.valueOf(PaletteManager.getPurpleBrand(context))
        typeface = TariFont.getFromAttributeSet(context, attrs)
    }
}