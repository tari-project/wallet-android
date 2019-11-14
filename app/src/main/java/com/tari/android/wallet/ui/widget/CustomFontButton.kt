package com.tari.android.wallet.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class CustomFontButton(context: Context, attrs: AttributeSet) :
    AppCompatButton(context, attrs) {

    private val sScheme = "http://schemas.android.com/apk/res-auto"
    private val sAttribute = "customFont"

    init {
        if (!isInEditMode) {
            val fontName = attrs.getAttributeValue(sScheme, sAttribute)
            requireNotNull(fontName) { "You must provide \"$sAttribute\" for your button." }
            val customTypeface = CustomFont.fromString(fontName).asTypeface(context)
            typeface = customTypeface
        }
    }
}