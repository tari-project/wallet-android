package com.tari.android.wallet.ui.fragment.settings.themeSelector

import com.tari.android.wallet.R

enum class TariTheme(val text: Int, val image: Int) {
    AppBased(R.string.select_theme_app_based, R.drawable.tari_theme_system_based),
    Light(R.string.select_theme_light, R.drawable.tari_theme_light),
    Dark(R.string.select_theme_dark, R.drawable.tari_theme_dark),
    Purple(R.string.select_theme_purple, R.drawable.tari_theme_purple),
}