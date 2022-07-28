package com.tari.android.wallet.ui.common.gyphy.placeholder

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.facebook.drawee.drawable.RoundedColorDrawable
import kotlin.math.abs

internal class GifColorPlaceholder private constructor(@field:ColorInt @param:ColorInt private val color: Int, private val cornerRadius: Float) :
    GifPlaceholder {
    override fun asDrawable(): Drawable = RoundedColorDrawable(cornerRadius, color)

    companion object {
        private const val DEFAULT_CORNER_RADIUS = 20f
        private val BUILT_IN_COLORS = intArrayOf(-0xff481b, -0x1a25ad, -0x76d11b, -0xff1a77, -0x1aa3a4)

        @JvmOverloads
        fun generate(target: Any, cornerRadius: Float = DEFAULT_CORNER_RADIUS): GifColorPlaceholder {
            return GifColorPlaceholder(BUILT_IN_COLORS[abs(target.hashCode()) % BUILT_IN_COLORS.size], cornerRadius)
        }
    }
}