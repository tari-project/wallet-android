package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute

abstract class TariBackground(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    fun setFullBack(r: Float, elevation: Float, backColor: Int) {
        when {
            elevation != 0.0F -> {
                val shadowColor = context.colorFromAttribute(R.attr.palette_shadow_box)
                outlineSpotShadowColor = shadowColor
                this.elevation = elevation
                this.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View?, outline: Outline?) {
                        outline?.setRoundRect(0, 0, view!!.width, view.height, r)
                    }
                }
                setWithRadius(r, backColor)
            }
            r != 0.0F -> setWithRadius(r, backColor)
            else -> setBackgroundColor(backColor)
        }
    }

    private fun setWithRadius(r: Float, backColor: Int) {
        val shapeDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        shapeDrawable.paint.color = backColor
        background = shapeDrawable
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? ViewGroup)?.let {
            it.clipToPadding = false
        }
    }
}