package com.tari.android.wallet.ui.component.tari.background

import android.content.Context
import android.util.AttributeSet
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.util.extension.obtain
import com.tari.android.wallet.util.extension.runRecycle

class TariSecondaryBackground(context: Context, attrs: AttributeSet) : TariBackground(context, attrs) {

    init {
        val backColor = PaletteManager.getBackgroundSecondary(context)

        obtain(attrs, R.styleable.TariSecondaryBackground).runRecycle {
            val r = getDimension(R.styleable.TariSecondaryBackground_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariSecondaryBackground_elevation, 0.0F)
            setFullBack(r, elevation, backColor)
        }
    }
}