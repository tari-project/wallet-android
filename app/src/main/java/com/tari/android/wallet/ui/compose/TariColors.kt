package com.tari.android.wallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TariColors(
    // Text Colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,

    // Primary Colors
    val primaryMain: Color,
    val primaryDark: Color, // Used for hover states. Reflects the primary.dark variable from the theme object
    val primaryLight: Color,
    val primaryContrast: Color,
    val primaryHover: Color, // Used for hover states. The token represents the value of action.hoverOpacity (0.04 by default) of the main token.
    val primarySelected: Color, // Used for selected states. The token represents the value of action.selectedOpacity (0.08 by default) of the main token.
    val primaryFocus: Color, // Used for focus states. The token represents the value of action.focusOpacity (0.12 by default) of the main token.
    val primaryFocusVisible: Color, // Used for focus visible states. The token represents the value of focusVisibleOpacity (0.3 by default) of the main token.
    val primaryOutlinedBorder: Color, // Used for enabled states (e.g., Button outlined variant). The token represents the value of outlinedBorderOpacity (0.5 by default) of the main token.

    // Secondary Colors
    val secondaryMain: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val secondaryContrast: Color,

    // Success Colors
    val successMain: Color,
    val successDark: Color,
    val successLight: Color,
    val successContrast: Color,

    // Error Colors
    val errorMain: Color,
    val errorDark: Color,
    val errorLight: Color,
    val errorContrast: Color,

    // Warning Colors
    val warningMain: Color,
    val warningDark: Color,
    val warningLight: Color,
    val warningContrast: Color,

    // Info Colors
    val infoMain: Color,
    val infoDark: Color,
    val infoLight: Color,
    val infoContrast: Color,

    // Background Colors
    val backgroundPrimary: Color, // Used for background of top layer elements
    val backgroundSecondary: Color, // Used for background of the lower layer of the app
    val backgroundAccent: Color, // Used for background of the lower layer of the app
    val backgroundPopup: Color, // Used for background of popups

    // Action Colors
    val actionActive: Color,
    val actionHover: Color,
    val actionSelected: Color,
    val actionDisabled: Color,
    val actionDisabledBackground: Color,
    val actionFocus: Color,

    val divider: Color,

    // Elevation
    val elevationOutlined: Color, // Used for outlining boxes/elements on the background

    // System Colors
    val systemGreen: Color,// Used for successful transactions
    val systemSecondaryGreen: Color, // Used for successful transactions background
    val systemYellow: Color, // Used for pending transactions
    val systemSecondaryYellow: Color, // Used for pending transactions background
    val systemRed: Color, // Used for outgoing transactions
    val systemSecondaryRed: Color, // Used for outgoing transactions background

    // Button Colors
    val buttonPrimaryBackground: Color,
    val buttonPrimaryText: Color,
    val buttonOutlined: Color,

    // Components Colors
    val componentsNavbarBackground: Color,
    val componentsNavbarIcons: Color,
    val componentsChipBackground: Color,
    val componentsChipText: Color,
)

val LocalTariColors = staticCompositionLocalOf {
    TariColors(
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textDisabled = Color.Unspecified,

        primaryMain = Color.Unspecified,
        primaryDark = Color.Unspecified,
        primaryLight = Color.Unspecified,
        primaryContrast = Color.Unspecified,
        primaryHover = Color.Unspecified,
        primarySelected = Color.Unspecified,
        primaryFocus = Color.Unspecified,
        primaryFocusVisible = Color.Unspecified,
        primaryOutlinedBorder = Color.Unspecified,

        secondaryMain = Color.Unspecified,
        secondaryDark = Color.Unspecified,
        secondaryLight = Color.Unspecified,
        secondaryContrast = Color.Unspecified,

        successMain = Color.Unspecified,
        successDark = Color.Unspecified,
        successLight = Color.Unspecified,
        successContrast = Color.Unspecified,

        errorMain = Color.Unspecified,
        errorDark = Color.Unspecified,
        errorLight = Color.Unspecified,
        errorContrast = Color.Unspecified,

        warningMain = Color.Unspecified,
        warningDark = Color.Unspecified,
        warningLight = Color.Unspecified,
        warningContrast = Color.Unspecified,

        infoMain = Color.Unspecified,
        infoDark = Color.Unspecified,
        infoLight = Color.Unspecified,
        infoContrast = Color.Unspecified,

        backgroundPrimary = Color.Unspecified,
        backgroundSecondary = Color.Unspecified,
        backgroundAccent = Color.Unspecified,
        backgroundPopup = Color.Unspecified,

        actionActive = Color.Unspecified,
        actionHover = Color.Unspecified,
        actionSelected = Color.Unspecified,
        actionDisabled = Color.Unspecified,
        actionDisabledBackground = Color.Unspecified,
        actionFocus = Color.Unspecified,

        divider = Color.Unspecified,

        elevationOutlined = Color.Unspecified,

        systemGreen = Color.Unspecified,
        systemSecondaryGreen = Color.Unspecified,
        systemYellow = Color.Unspecified,
        systemSecondaryYellow = Color.Unspecified,
        systemRed = Color.Unspecified,
        systemSecondaryRed = Color.Unspecified,

        buttonPrimaryBackground = Color.Unspecified,
        buttonPrimaryText = Color.Unspecified,
        buttonOutlined = Color.Unspecified,

        componentsNavbarBackground = Color.Unspecified,
        componentsNavbarIcons = Color.Unspecified,
        componentsChipBackground = Color.Unspecified,
        componentsChipText = Color.Unspecified,
    )
}

val TariLightColorPalette = TariColors(
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF7F8599),
    textDisabled = Color(0x66000000),

    primaryMain = Color(0xFFC9EB00),
    primaryDark = Color(0xFF95B500),
    primaryLight = Color(0xFFDFFB20),
    primaryContrast = Color(0xFFFFFFFF),
    primaryHover = Color(0x0A95B500),
    primarySelected = Color(0x1495B500),
    primaryFocus = Color(0x1F95B500),
    primaryFocusVisible = Color(0x4D95B500),
    primaryOutlinedBorder = Color(0x8095B500),

    secondaryMain = Color(0xFF9330FF),
    secondaryDark = Color(0xFF750EE2),
    secondaryLight = Color(0xFF9F42FF),
    secondaryContrast = Color(0xFFFFFFFF),

    successMain = Color(0xFF00C047),
    successDark = Color(0xFF00963C),
    successLight = Color(0xFF03FE66),
    successContrast = Color(0xFFFFFFFF),

    errorMain = Color(0xFFFF3232),
    errorDark = Color(0xFFED1515),
    errorLight = Color(0xFFFF6464),
    errorContrast = Color(0xFFFFFFFF),

    warningMain = Color(0xFFECA86A),
    warningDark = Color(0xFFE88F4F),
    warningLight = Color(0xFFF5D4B3),
    warningContrast = Color(0xFFFFFFFF),

    infoMain = Color(0xFF318EFA),
    infoDark = Color(0xFF195CDC),
    infoLight = Color(0xFF5DB2FD),
    infoContrast = Color(0xFFFFFFFF),

    backgroundPrimary = Color(0xFFFFFFFF),
    backgroundSecondary = Color(0xFFF8F8F9),
    backgroundAccent = Color(0x1B19210A),
    backgroundPopup = Color(0xFFF8F8F9),

    actionActive = Color(0x80000000),
    actionHover = Color(0x0D000000),
    actionSelected = Color(0x1A000000),
    actionDisabled = Color(0x66000000),
    actionDisabledBackground = Color(0x0D000000),
    actionFocus = Color(0x1A000000),

    divider = Color(0x14000000),

    elevationOutlined = Color(0xFFEEEEF0),

    systemGreen = Color(0xFF03FE66),
    systemSecondaryGreen = Color(0xFFEDFFF3),
    systemYellow = Color(0xFFFCAA2F),
    systemSecondaryYellow = Color(0xFFFBE7C0),
    systemRed = Color(0xFFFF3232),
    systemSecondaryRed = Color(0xFFF9E1E4),

    buttonPrimaryBackground = Color(0xFF000000),
    buttonPrimaryText = Color(0xFFFFFFFF),
    buttonOutlined = Color(0xFF000000),

    componentsNavbarBackground = Color(0xFFFFFFFF),
    componentsNavbarIcons = Color(0xFF141B34),
    componentsChipBackground = Color(0xFF000000),
    componentsChipText = Color(0xFFFFFFFF),
)

val TariDarkColorPalette = TariColors(
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB6B7C3),
    textDisabled = Color(0x66FFFFFF),

    primaryMain = Color(0xFFC9EB00),
    primaryDark = Color(0xFFDFFB20),
    primaryLight = Color(0xFF95B500),
    primaryContrast = Color(0xFFFFFFFF),
    primaryHover = Color(0x14DFFB20),
    primarySelected = Color(0x29DFFB20),
    primaryFocus = Color(0x1FDFFB20),
    primaryFocusVisible = Color(0x4DDFFB20),
    primaryOutlinedBorder = Color(0x80DFFB20),

    secondaryMain = Color(0xFF9F42FF),
    secondaryDark = Color(0xFFBA76FF),
    secondaryLight = Color(0xFFF9F4FF),
    secondaryContrast = Color(0xFFFFFFFF),

    successMain = Color(0xFF03FE66),
    successDark = Color(0xFF2CFC7D),
    successLight = Color(0xFF00C047),
    successContrast = Color(0xFFFFFFFF),

    errorMain = Color(0xFFFF3232),
    errorDark = Color(0xFFFF6464),
    errorLight = Color(0xFFED1515),
    errorContrast = Color(0xFFFFFFFF),

    warningMain = Color(0xFFECA86A),
    warningDark = Color(0xFFF5D4B3),
    warningLight = Color(0xFFE88F4F),
    warningContrast = Color(0xFFFFFFFF),

    infoMain = Color(0xFF5DB2FD),
    infoDark = Color(0xFF91CEFF),
    infoLight = Color(0xFF195CDC),
    infoContrast = Color(0xFFFFFFFF),

    backgroundPrimary = Color(0xFF161617),
    backgroundSecondary = Color(0xFF000000),
    backgroundAccent = Color(0x14FFFFFF),
    backgroundPopup = Color(0xFF1D1D1D),

    actionActive = Color(0x80FFFFFF),
    actionHover = Color(0x1AFFFFFF),
    actionSelected = Color(0x26FFFFFF),
    actionDisabled = Color(0x66FFFFFF),
    actionDisabledBackground = Color(0x1AFFFFFF),
    actionFocus = Color(0x1AFFFFFF),

    divider = Color(0x1FFFFFFF),

    elevationOutlined = Color(0x1FFFFFFF),

    systemGreen = Color(0xFF03FE66),
    systemSecondaryGreen = Color(0xFFEDFFF3),
    systemYellow = Color(0xFFFCAA2F),
    systemSecondaryYellow = Color(0xFFFBE7C0),
    systemRed = Color(0xFFFF3232),
    systemSecondaryRed = Color(0xFFF9E1E4),

    buttonPrimaryBackground = Color(0xFFF9F9F9),
    buttonPrimaryText = Color(0xFF000000),
    buttonOutlined = Color(0xFFFFFFFF),

    componentsNavbarBackground = Color(0xFF1D1D1D),
    componentsNavbarIcons = Color(0xFFFFFFFF),
    componentsChipBackground = Color(0xFFF9F9F9),
    componentsChipText = Color(0xFF000000),
)
