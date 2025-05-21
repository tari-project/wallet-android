package com.tari.android.wallet.ui.component.tari.background

import android.content.Context
import android.util.AttributeSet
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.util.extension.obtain
import com.tari.android.wallet.util.extension.runRecycle

class TariSwitchedBackground(context: Context, attrs: AttributeSet) : TariBackground(context, attrs) {

    private var isTurnedOn = false

    init {
        val backColor = PaletteManager.getBackgroundPrimary(context)

        obtain(attrs, R.styleable.TariQrBackground).runRecycle {
            isTurnedOn = getBoolean(R.styleable.TariSwitchedBackground_turnedOn, false)
            val r = getDimension(R.styleable.TariSwitchedBackground_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariSwitchedBackground_elevation, 0.0F)
            setFullBack(r, elevation, backColor)
            applySwitch()
        }
    }

    private fun applySwitch() {
        if (isTurnedOn) {
            restoreFullBack()
        } else {
            reset()
        }
    }
}