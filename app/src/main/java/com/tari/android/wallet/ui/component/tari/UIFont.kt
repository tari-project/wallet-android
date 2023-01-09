package com.tari.android.wallet.ui.component.tari

enum class UIFont {
    Black,
    Heavy,
    Medium,
    Regular,
    Light,
    Roman;

    fun toTariFont(): TariFont = when (this) {
        Black -> TariFont.AVENIR_LT_STD_BLACK
        Heavy -> TariFont.AVENIR_LT_STD_HEAVY
        Medium -> TariFont.AVENIR_LT_STD_MEDIUM
        Regular -> TariFont.AVENIR_NEXT_LT_PRO_REGULAR
        Light -> TariFont.AVENIR_LT_STD_LIGHT
        Roman -> TariFont.AVENIR_LT_STD_ROMAN
    }
}