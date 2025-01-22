package com.tari.android.wallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * All the colors used in the current Tari design system. Will be obsolete once the new design system is fully implemented.
 * FIXME: Remove this class once the new design system is fully implemented.
 */
@Immutable
data class TariObsoleteColors(
    val neutralPrimary: Color,
    val neutralSecondary: Color,
    val neutralTertiary: Color,
    val neutralInactive: Color,
    val buttonPrimaryStart: Color,
    val buttonPrimaryEnd: Color,
    val buttonDisable: Color,
    val buttonDisabledText: Color,
    val textHeading: Color,
    val textBody: Color,
    val textLight: Color,
    val iconsDefault: Color,
    val iconsInactive: Color,
    val overlay: Color,
    val overlayText: Color,
    val systemSecondaryRed: Color,
    val systemSecondaryOrange: Color,
    val systemSecondaryYellow: Color,
    val systemSecondaryGreen: Color,
    val systemSecondaryBlue: Color,
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val shadowBox: Color,
    val brandPurple: Color = TariObsoleteColorPalette.brandPurple,
    val brandPink: Color = TariObsoleteColorPalette.brandPink,
    val brandDarkBlue: Color = TariObsoleteColorPalette.brandDarkBlue,
    val buttonPrimaryText: Color = TariObsoleteColorPalette.buttonPrimaryText,
    val textLinks: Color = TariObsoleteColorPalette.textLinks,
    val iconsActive: Color = TariObsoleteColorPalette.iconsActive,
    val qrBackground: Color = TariObsoleteColorPalette.qrBackground,
    val systemRed: Color = TariObsoleteColorPalette.systemRed,
    val systemOrange: Color = TariObsoleteColorPalette.systemOrange,
    val systemYellow: Color = TariObsoleteColorPalette.systemYellow,
    val systemGreen: Color = TariObsoleteColorPalette.systemGreen,
    val systemBlue: Color = TariObsoleteColorPalette.systemBlue,
)

val LocalTariObsoleteColors = staticCompositionLocalOf {
    TariObsoleteColors(
        neutralPrimary = Color.Unspecified,
        neutralSecondary = Color.Unspecified,
        neutralTertiary = Color.Unspecified,
        neutralInactive = Color.Unspecified,
        buttonPrimaryStart = Color.Unspecified,
        buttonPrimaryEnd = Color.Unspecified,
        buttonDisable = Color.Unspecified,
        buttonDisabledText = Color.Unspecified,
        textHeading = Color.Unspecified,
        textBody = Color.Unspecified,
        textLight = Color.Unspecified,
        iconsDefault = Color.Unspecified,
        iconsInactive = Color.Unspecified,
        overlay = Color.Unspecified,
        overlayText = Color.Unspecified,
        systemSecondaryRed = Color.Unspecified,
        systemSecondaryOrange = Color.Unspecified,
        systemSecondaryYellow = Color.Unspecified,
        systemSecondaryGreen = Color.Unspecified,
        systemSecondaryBlue = Color.Unspecified,
        backgroundPrimary = Color.Unspecified,
        backgroundSecondary = Color.Unspecified,
        shadowBox = Color.Unspecified,
        brandPurple = TariObsoleteColorPalette.brandPurple,
        brandPink = TariObsoleteColorPalette.brandPink,
        brandDarkBlue = TariObsoleteColorPalette.brandDarkBlue,
        buttonPrimaryText = TariObsoleteColorPalette.buttonPrimaryText,
        textLinks = TariObsoleteColorPalette.textLinks,
        iconsActive = TariObsoleteColorPalette.iconsActive,
        qrBackground = TariObsoleteColorPalette.qrBackground,
        systemRed = TariObsoleteColorPalette.systemRed,
        systemOrange = TariObsoleteColorPalette.systemOrange,
        systemYellow = TariObsoleteColorPalette.systemYellow,
        systemGreen = TariObsoleteColorPalette.systemGreen,
        systemBlue = TariObsoleteColorPalette.systemBlue,
    )
}

val TariObsoleteLightColors = TariObsoleteColors(
    neutralPrimary = TariObsoleteColorPalette.neutralPrimaryLight,
    neutralSecondary = TariObsoleteColorPalette.neutralSecondaryLight,
    neutralTertiary = TariObsoleteColorPalette.neutralTertiaryLight,
    neutralInactive = TariObsoleteColorPalette.neutralInactiveLight,
    buttonPrimaryStart = TariObsoleteColorPalette.buttonPrimaryStartLight,
    buttonPrimaryEnd = TariObsoleteColorPalette.buttonPrimaryEndLight,
    buttonDisable = TariObsoleteColorPalette.buttonDisableLight,
    buttonDisabledText = TariObsoleteColorPalette.buttonDisabledTextLight,
    textHeading = TariObsoleteColorPalette.textHeadingLight,
    textBody = TariObsoleteColorPalette.textBodyLight,
    textLight = TariObsoleteColorPalette.textLightLight,
    iconsDefault = TariObsoleteColorPalette.iconsDefaultLight,
    iconsInactive = TariObsoleteColorPalette.iconsInactiveLight,
    overlay = TariObsoleteColorPalette.overlayLight,
    overlayText = TariObsoleteColorPalette.overlayTextLight,
    systemSecondaryRed = TariObsoleteColorPalette.systemSecondaryRedLight,
    systemSecondaryOrange = TariObsoleteColorPalette.systemSecondaryOrangeLight,
    systemSecondaryYellow = TariObsoleteColorPalette.systemSecondaryYellowLight,
    systemSecondaryGreen = TariObsoleteColorPalette.systemSecondaryGreenLight,
    systemSecondaryBlue = TariObsoleteColorPalette.systemSecondaryBlueLight,
    backgroundPrimary = TariObsoleteColorPalette.backgroundPrimaryLight,
    backgroundSecondary = TariObsoleteColorPalette.backgroundSecondaryLight,
    shadowBox = TariObsoleteColorPalette.shadowBoxLight,
)

val TariObsoleteDarkColors = TariObsoleteColors(
    neutralPrimary = TariObsoleteColorPalette.neutralPrimaryDark,
    neutralSecondary = TariObsoleteColorPalette.neutralSecondaryDark,
    neutralTertiary = TariObsoleteColorPalette.neutralTertiaryDark,
    neutralInactive = TariObsoleteColorPalette.neutralInactiveDark,
    buttonPrimaryStart = TariObsoleteColorPalette.buttonPrimaryStartDark,
    buttonPrimaryEnd = TariObsoleteColorPalette.buttonPrimaryEndDark,
    buttonDisable = TariObsoleteColorPalette.buttonDisableDark,
    buttonDisabledText = TariObsoleteColorPalette.buttonDisabledTextDark,
    textHeading = TariObsoleteColorPalette.textHeadingDark,
    textBody = TariObsoleteColorPalette.textBodyDark,
    textLight = TariObsoleteColorPalette.textLightDark,
    iconsDefault = TariObsoleteColorPalette.iconsDefaultDark,
    iconsInactive = TariObsoleteColorPalette.iconsInactiveDark,
    overlay = TariObsoleteColorPalette.overlayDark,
    overlayText = TariObsoleteColorPalette.overlayTextDark,
    systemSecondaryRed = TariObsoleteColorPalette.systemSecondaryRedDark,
    systemSecondaryOrange = TariObsoleteColorPalette.systemSecondaryOrangeDark,
    systemSecondaryYellow = TariObsoleteColorPalette.systemSecondaryYellowDark,
    systemSecondaryGreen = TariObsoleteColorPalette.systemSecondaryGreenDark,
    systemSecondaryBlue = TariObsoleteColorPalette.systemSecondaryBlueDark,
    backgroundPrimary = TariObsoleteColorPalette.backgroundPrimaryDark,
    backgroundSecondary = TariObsoleteColorPalette.backgroundSecondaryDark,
    shadowBox = TariObsoleteColorPalette.shadowBoxDark,
)

val TariObsoletePurpleColors = TariObsoleteColors(
    neutralPrimary = TariObsoleteColorPalette.neutralPrimaryPurple,
    neutralSecondary = TariObsoleteColorPalette.neutralSecondaryPurple,
    neutralTertiary = TariObsoleteColorPalette.neutralTertiaryPurple,
    neutralInactive = TariObsoleteColorPalette.neutralInactivePurple,
    buttonPrimaryStart = TariObsoleteColorPalette.buttonPrimaryStartPurple,
    buttonPrimaryEnd = TariObsoleteColorPalette.buttonPrimaryEndPurple,
    buttonDisable = TariObsoleteColorPalette.buttonDisablePurple,
    buttonDisabledText = TariObsoleteColorPalette.buttonDisabledTextPurple,
    textHeading = TariObsoleteColorPalette.textHeadingPurple,
    textBody = TariObsoleteColorPalette.textBodyPurple,
    textLight = TariObsoleteColorPalette.textLightPurple,
    iconsDefault = TariObsoleteColorPalette.iconsDefaultPurple,
    iconsInactive = TariObsoleteColorPalette.iconsInactivePurple,
    overlay = TariObsoleteColorPalette.overlayPurple,
    overlayText = TariObsoleteColorPalette.overlayTextPurple,
    systemSecondaryRed = TariObsoleteColorPalette.systemSecondaryRedPurple,
    systemSecondaryOrange = TariObsoleteColorPalette.systemSecondaryOrangePurple,
    systemSecondaryYellow = TariObsoleteColorPalette.systemSecondaryYellowPurple,
    systemSecondaryGreen = TariObsoleteColorPalette.systemSecondaryGreenPurple,
    systemSecondaryBlue = TariObsoleteColorPalette.systemSecondaryBluePurple,
    backgroundPrimary = TariObsoleteColorPalette.backgroundPrimaryPurple,
    backgroundSecondary = TariObsoleteColorPalette.backgroundSecondaryPurple,
    shadowBox = TariObsoleteColorPalette.shadowBoxPurple,
)

object TariObsoleteColorPalette {
    val neutralPrimaryDark = Color(0xFF060606)
    val neutralSecondaryDark = Color(0xFF181818)
    val neutralTertiaryDark = Color(0xFF222222)
    val neutralInactiveDark = Color(0xFF28292D)
    val buttonPrimaryStartDark = Color(0xFF9330FF)
    val buttonPrimaryEndDark = Color(0xFF3A0470)
    val buttonDisableDark = Color(0xFF222222)
    val buttonDisabledTextDark = Color(0xFF545454)
    val textHeadingDark = Color(0xFFFFFFFF)
    val textBodyDark = Color(0xFFC3C7D7)
    val textLightDark = Color(0xFF646B84)
    val iconsDefaultDark = Color(0xFFFFFFFF)
    val iconsInactiveDark = Color(0xFF646B84)
    val overlayDark = Color(0xFF28292D)
    val overlayTextDark = Color(0xFFFFFFFF)
    val systemSecondaryRedDark = Color(0xFF222222)
    val systemSecondaryOrangeDark = Color(0xFF222222)
    val systemSecondaryYellowDark = Color(0xFF222222)
    val systemSecondaryGreenDark = Color(0xFF222222)
    val systemSecondaryBlueDark = Color(0xFF222222)
    val backgroundPrimaryDark = Color(0xFF060606)
    val backgroundSecondaryDark = Color(0xFF181818)
    val shadowBoxDark = Color(0xFFFFFFFF)

    val neutralPrimaryLight = Color(0xFFFFFFFF)
    val neutralSecondaryLight = Color(0xFFF6F6F6)
    val neutralTertiaryLight = Color(0xFFEEEEF0)
    val neutralInactiveLight = Color(0xFFE1E1E1)
    val buttonPrimaryStartLight = Color(0xFF6239FF)
    val buttonPrimaryEndLight = Color(0xFFE320BC)
    val buttonDisableLight = Color(0xFFECECEC)
    val buttonDisabledTextLight = Color(0xFFB6B6B6)
    val textHeadingLight = Color(0xFF000000)
    val textBodyLight = Color(0xFF7F8599)
    val textLightLight = Color(0xFFC3C7D7)
    val iconsDefaultLight = Color(0xFF000000)
    val iconsInactiveLight = Color(0xFFC3C7D7)
    val overlayLight = Color(0xFFFFFFFF)
    val overlayTextLight = Color(0xFF9330FF)
    val systemSecondaryRedLight = Color(0xFFF9E1E4)
    val systemSecondaryOrangeLight = Color(0xFFFFDCCD)
    val systemSecondaryYellowLight = Color(0xFFFBE7C0)
    val systemSecondaryGreenLight = Color(0xFFC8F5E4)
    val systemSecondaryBlueLight = Color(0xFFA8E5F8)
    val backgroundPrimaryLight = Color(0xFFFFFFFF)
    val backgroundSecondaryLight = Color(0xFFF5F5F7)
    val shadowBoxLight = Color(0xFF000000)

    val neutralPrimaryPurple = Color(0xFF1D003E)
    val neutralSecondaryPurple = Color(0xFF280055)
    val neutralTertiaryPurple = Color(0xFF3A007B)
    val neutralInactivePurple = Color(0xFF3A007B)
    val buttonPrimaryStartPurple = Color(0xFF9330FF)
    val buttonPrimaryEndPurple = Color(0xFF3A0470)
    val buttonDisablePurple = Color(0xFF2B0557)
    val buttonDisabledTextPurple = Color(0xFF4E257A)
    val textHeadingPurple = Color(0xFFFFFFFF)
    val textBodyPurple = Color(0xFFC3C7D7)
    val textLightPurple = Color(0xFF646B84)
    val iconsDefaultPurple = Color(0xFFFFFFFF)
    val iconsInactivePurple = Color(0xFF646B84)
    val overlayPurple = Color(0xFF3A007B)
    val overlayTextPurple = Color(0xFFFFFFFF)
    val systemSecondaryRedPurple = Color(0xFF310068)
    val systemSecondaryOrangePurple = Color(0xFF310068)
    val systemSecondaryYellowPurple = Color(0xFF310068)
    val systemSecondaryGreenPurple = Color(0xFF310068)
    val systemSecondaryBluePurple = Color(0xFF310068)
    val backgroundPrimaryPurple = Color(0xFF1D003E)
    val backgroundSecondaryPurple = Color(0xFF280055)
    val shadowBoxPurple = Color(0xFFC290F8)

    val brandPurple = Color(0xFF9330FF)
    val brandPink = Color(0xFFE320BC)
    val brandDarkBlue = Color(0xFF40388A)
    val buttonPrimaryText = Color(0xFFFFFFFF)
    val textLinks = Color(0xFF9330FF)
    val iconsActive = Color(0xFF9330FF)
    val qrBackground = Color(0xFFFFFFFF)
    val systemRed = Color(0xFFD85240)
    val systemOrange = Color(0xFFC36928)
    val systemYellow = Color(0xFFD18A18)
    val systemGreen = Color(0xFF23BE90)
    val systemBlue = Color(0xFF4D6FE8)
}