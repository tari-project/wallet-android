package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle

class TariQrBackground(context: Context, attrs: AttributeSet) : TariBackground(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_qr_background)

        obtain(attrs, R.styleable.TariQrBackground).runRecycle {
            val r = getDimension(R.styleable.TariQrBackground_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariQrBackground_elevation, 0.0F)
            setFullBack(r, elevation, backColor)
        }
    }
}