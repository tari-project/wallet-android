package com.tari.android.wallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.tari.android.wallet.R

@Immutable
data class TariTextStyles(
    val heading2XLarge: TextStyle = TextStyle.Default,
    val headingXLarge: TextStyle = TextStyle.Default,
    val headingLarge: TextStyle = TextStyle.Default,
    val headingMedium: TextStyle = TextStyle.Default,
    val headingSmall: TextStyle = TextStyle.Default,
    val body1: TextStyle = TextStyle.Default,
    val body2: TextStyle = TextStyle.Default,
    val modalTitleLarge: TextStyle = TextStyle.Default,
    val modalTitle: TextStyle = TextStyle.Default,
    val menuItem: TextStyle = TextStyle.Default,
    val buttonText: TextStyle = TextStyle.Default,
    val buttonLarge: TextStyle = TextStyle.Default,
    val buttonMedium: TextStyle = TextStyle.Default,
    val buttonSmall: TextStyle = TextStyle.Default,
    val linkSpan: TextLinkStyles = TextLinkStyles(),
)

val LocalTariTextStyles = compositionLocalOf { TariTextStyles() }

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_black, FontWeight.Black),
    Font(R.font.poppins_blackitalic, FontWeight.Black, FontStyle.Italic),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    Font(R.font.poppins_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.poppins_extralight, FontWeight.ExtraLight),
    Font(R.font.poppins_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.poppins_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_lightitalic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.poppins_thin, FontWeight.Thin),
    Font(R.font.poppins_thinitalic, FontWeight.Thin, FontStyle.Italic),
)