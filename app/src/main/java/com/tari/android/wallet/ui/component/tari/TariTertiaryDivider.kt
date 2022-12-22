package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.tari.android.wallet.ui.common.domain.PaletteManager

class TariTertiaryDivider(context: Context, attrs: AttributeSet) : View(context, attrs) {

    init {
        val backColor = PaletteManager().getNeutralTertiary(context)
        setBackgroundColor(backColor)
    }
}