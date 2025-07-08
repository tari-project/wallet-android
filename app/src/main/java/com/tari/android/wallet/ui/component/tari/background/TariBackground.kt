package com.tari.android.wallet.ui.component.tari.background

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.tari.android.wallet.ui.common.domain.PaletteManager

abstract class TariBackground(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    protected var radius = 0.0F
    protected var backColor: Int? = null
    protected var backElevation = 0.0F

    fun setFullBack(r: Float, elevation: Float, backColor: Int?) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val shadowColor = PaletteManager.getShadowBox(context)
                    outlineSpotShadowColor = shadowColor
                    this.outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View?, outline: Outline?) {
                            outline?.setRoundRect(0, 0, view!!.width, view.height, radius)
                        }
                    }
                }
                setWithRadius(radius, backColor)
                elevation = backElevation
            }

            radius != 0.0F -> setWithRadius(radius, backColor)
            backColor != null -> setBackgroundColor(backColor!!)
        }
    }

    private fun setWithRadius(r: Float, backColor: Int?) {
        val shapeDrawable = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        backColor?.let { shapeDrawable.paint.color = it }
        background = shapeDrawable
    }

    fun reset() {
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