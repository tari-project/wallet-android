package com.tari.android.wallet.ui.common.domain

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.colorFromAttribute

object PaletteManager {

    fun getTextHeading(context: Context): Int = context.colorFromAttribute(R.attr.palette_text_heading)

    fun getTextBody(context: Context): Int = context.colorFromAttribute(R.attr.palette_text_body)

    fun getTextLinks(context: Context): Int = context.colorFromAttribute(R.attr.palette_text_links)

    fun getRed(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_red)

    fun getYellow(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_yellow)

    fun getOrange(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_orange)

    fun getGreen(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_green)

    fun getSecondaryRed(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_secondary_red)

    fun getSecondaryYellow(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_secondary_yellow)

    fun getSecondaryOrange(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_secondary_orange)

    fun getSecondaryGreen(context: Context): Int = context.colorFromAttribute(R.attr.palette_system_secondary_green)

    fun getPurpleBrand(context: Context): Int = context.colorFromAttribute(R.attr.palette_brand_purple)

    fun getNeutralSecondary(context: Context) = context.colorFromAttribute(R.attr.palette_neutral_secondary)

    fun getNeutralTertiary(context: Context) = context.colorFromAttribute(R.attr.palette_neutral_tertiary)

    fun getBackgroundPrimary(context: Context) = context.colorFromAttribute(R.attr.palette_background_primary)

    fun getBackgroundSecondary(context: Context) = context.colorFromAttribute(R.attr.palette_background_secondary)

    fun getButtonPrimaryText(context: Context) = context.colorFromAttribute(R.attr.palette_button_primary_text)

    fun getButtonDisabled(context: Context) = context.colorFromAttribute(R.attr.palette_button_disable)

    fun getOverlayText(context: Context) = context.colorFromAttribute(R.attr.palette_overlay_text)

    fun getBackgroundQr(context: Context) = context.colorFromAttribute(R.attr.palette_qr_background)

    fun getIconDefault(context: Context) = context.colorFromAttribute(R.attr.palette_icons_default)

    fun getIconInactive(context: Context) = context.colorFromAttribute(R.attr.palette_icons_inactive)

    fun getShadowBox(context: Context) = context.colorFromAttribute(R.attr.palette_shadow_box)

    fun getWhite(context: Context): Int = context.color(R.color.white)

    fun getBlack(context: Context): Int = context.color(R.color.black)

    fun getLightGray(context: Context): Int = context.color(R.color.light_gray)
}