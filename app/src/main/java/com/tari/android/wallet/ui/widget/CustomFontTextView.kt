package com.tari.android.wallet.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class CustomFontTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    private val sScheme = "http://schemas.android.com/apk/res-auto"
    private val sAttribute = "customFont"

    init {
        if (!isInEditMode) {
            val fontName = attrs.getAttributeValue(sScheme, sAttribute)
            requireNotNull(fontName) { "You must provide \"$sAttribute\" for your text view." }
            val customTypeface = CustomFont.fromString(fontName).asTypeface(context)
            typeface = customTypeface
        }
    }
}