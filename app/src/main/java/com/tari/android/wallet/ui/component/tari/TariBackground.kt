package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.tari.android.wallet.ui.common.domain.PaletteManager

abstract class TariBackground(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    protected var radius = 0.0F
    protected var backColor = 0
    protected var backElevation = 0.0F

    fun setFullBack(r: Float, elevation: Float, backColor: Int) {
        this.radius = r
        this.backElevation = elevation
        this.backColor = backColor

        updateBack()
    }

    fun restoreFullBack() {
        updateBack()
    }

    fun updateBack(r: Float? = null, elevation: Float? = null, backColor: Int? = null) {
        r?.let { radius = it }
        elevation?.let { backElevation = it }
        backColor?.let { this.backColor = it }
        updateBack()
    }

    private fun updateBack() {
        when {
            backElevation != 0.0F -> {
                val shadowColor = PaletteManager().getShadowBox(context)
                outlineSpotShadowColor = shadowColor
                this.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View?, outline: Outline?) {
                        outline?.setRoundRect(0, 0, view!!.width, view.height, radius)
                    }
                }
                setWithRadius(radius, backColor)
                elevation = backElevation
            }

            radius != 0.0F -> setWithRadius(radius, backColor)
            else -> setBackgroundColor(backColor)
        }
    }

    private fun setWithRadius(r: Float, backColor: Int) {
        val shapeDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        shapeDrawable.paint.color = backColor
        background = shapeDrawable
    }

    protected fun reset() {
        background = null
        elevation = 0.0F
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? ViewGroup)?.let {
            it.clipToPadding = false
        }
    }
}