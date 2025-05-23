package com.tari.android.wallet.ui.component.tari

enum class UIFont {
    Black,
    Heavy,
    Medium,
    Regular,
    Light,
    Roman;

    fun toTariFont(): TariFont = when (this) {
        Black -> TariFont.BLACK
        Heavy -> TariFont.HEAVY
        Medium -> TariFont.MEDIUM
        Regular -> TariFont.REGULAR
        Light -> TariFont.LIGHT
        Roman -> TariFont.ROMAN
    }
}