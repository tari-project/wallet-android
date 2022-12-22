package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.setColor

class TariProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    private val paletteManager = PaletteManager()

    init {
        setColor(paletteManager.getPurpleBrand(context))
    }

    fun setWhite() = setColor(paletteManager.getWhite(context))

    fun setError() = setColor(paletteManager.getRed(context))
}