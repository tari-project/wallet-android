package com.tari.android.wallet.ui.common.domain

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute

class PaletteManager {

    fun getTextHeading(context: Context): Int = context.colorFromAttribute(R.attr.palette_text_heading)

    fun getTextBody(context: Context): Int = context.colorFromAttribute(R.attr.palette_text_body)

    fun getRed(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_red)

    fun getBrandColor(context: Context): Int = context.colorFromAttribute(R.attr.palette_brand_purple)
}