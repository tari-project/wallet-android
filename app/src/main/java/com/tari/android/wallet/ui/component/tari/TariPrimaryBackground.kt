package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle

class TariPrimaryBackground(context: Context, attrs: AttributeSet) : TariBackground(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_background_primary)

        obtain(attrs, R.styleable.TariPrimaryBackground).runRecycle {
            val r = getDimension(R.styleable.TariPrimaryBackground_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariPrimaryBackground_elevation, 0.0F)
            setFullBack(r, elevation, backColor)
        }
    }
}