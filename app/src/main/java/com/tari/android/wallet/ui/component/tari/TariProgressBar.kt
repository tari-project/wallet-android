package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setColor

class TariProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        setColor(PaletteManager.getPurpleBrand(context))
    }

    fun setWhite() = setColor(PaletteManager.getWhite(context))

    fun setError() = setColor(PaletteManager.getRed(context))
}