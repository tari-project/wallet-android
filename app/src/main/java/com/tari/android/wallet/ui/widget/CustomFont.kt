package com.tari.android.wallet.ui.widget

import android.content.Context
import android.graphics.Typeface
import java.util.*

enum class CustomFont(val fileName: String) {
    AVENIR_NEXT_LT_PRO_REGULAR("fonts/AvenirNextLTPro-Regular.otf");

    companion object {
        fun fromString(fontName: String): CustomFont {
            return valueOf(fontName.toUpperCase(Locale.US))
        }
    }

    fun asTypeface(context: Context): Typeface {
        return Typeface.createFromAsset(context.assets, fileName)
    }

}