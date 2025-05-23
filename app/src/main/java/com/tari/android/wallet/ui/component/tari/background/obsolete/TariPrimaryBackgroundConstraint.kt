package com.tari.android.wallet.ui.component.tari.background.obsolete

import android.content.Context
import android.util.AttributeSet
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.util.extension.obtain
import com.tari.android.wallet.util.extension.runRecycle

@Deprecated("This is an obsolete class inheriting ConstraintLayout. Use TariPrimaryBackground instead.")
class TariPrimaryBackgroundConstraint(context: Context, attrs: AttributeSet) : TariBackgroundConstraint(context, attrs) {

    init {
        val backColor = PaletteManager.getBackgroundPrimary(context)

        obtain(attrs, R.styleable.TariPrimaryBackground).runRecycle {
            val r = getDimension(R.styleable.TariPrimaryBackground_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariPrimaryBackground_elevation, 0.0F)
            setFullBack(r, elevation, backColor)
        }
    }
}