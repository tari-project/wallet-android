/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.colorFromAttribute
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle

class TariLinearRound(context: Context, attrs: AttributeSet) : LinearLayoutCompat(context, attrs) {

    init {
        obtain(attrs, R.styleable.TariLinearRound).runRecycle {
            val r = getDimension(R.styleable.TariLinearRound_cornerRadius, 0.0F)
            val elevation = getDimension(R.styleable.TariLinearRound_elevation, 0.0F)
            val backColor = getColor(R.styleable.TariLinearRound_backgroundColor, 0)
            val shadowColor = context.colorFromAttribute(R.attr.palette_shadow_box)

            background = generateBackgroundWithShadow(this@TariLinearRound, backColor, r, shadowColor, elevation.toInt(), Gravity.BOTTOM)
        }
    }

    companion object {

        fun generateBackgroundWithShadow(
            view: View,
            backgroundColor: Int,
            cornerRadius: Float,
            shadowColor: Int,
            elevation: Int,
            shadowGravity: Int
        ): Drawable {

            val outerRadius = floatArrayOf(
                cornerRadius, cornerRadius, cornerRadius,
                cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                cornerRadius
            )

            val backgroundPaint = Paint().apply {
                style = Paint.Style.FILL
                setShadowLayer(cornerRadius, 0.0F, 0.0F, 0)
            }

            val shapeDrawablePadding = Rect().apply {
                left = elevation
                right = elevation
            }

            val dy: Float
            when (shadowGravity) {
                Gravity.CENTER -> {
                    shapeDrawablePadding.top = elevation
                    shapeDrawablePadding.bottom = elevation
                    dy = 0.0F
                }
                Gravity.TOP -> {
                    shapeDrawablePadding.top = elevation * 2
                    shapeDrawablePadding.bottom = elevation
                    dy = -1 * elevation.toFloat() / 3
                }
                else -> {
                    shapeDrawablePadding.top = elevation
                    shapeDrawablePadding.bottom = elevation * 2
                    dy = elevation.toFloat() / 3
                }
            }

            val shapeDrawable = ShapeDrawable().apply {
                setPadding(shapeDrawablePadding)

                paint.color = backgroundColor
                paint.setShadowLayer(cornerRadius / 3, 0.0F, dy, shadowColor)

                shape = RoundRectShape(outerRadius, null, null)
            }

            view.setLayerType(LAYER_TYPE_SOFTWARE, shapeDrawable.paint)

            val drawable = LayerDrawable(arrayOf(shapeDrawable))
            drawable.setLayerInset(0, elevation, elevation * 2, elevation, elevation * 2)

            return drawable
        }
    }
}