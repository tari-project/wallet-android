package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle

class TariPrimaryBackground(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        val backColor = context.colorFromAttribute(R.attr.palette_background_primary)

        obtain(attrs, R.styleable.TariPrimaryBackground).runRecycle {
            val r = getDimension(R.styleable.TariPrimaryBackground_cornerRadius, 0.0F)

            if (r != 0.0F) {
                val shapeDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
                shapeDrawable.paint.color = backColor
                background = shapeDrawable
            } else {
                setBackgroundColor(backColor)
            }
        }
    }
}