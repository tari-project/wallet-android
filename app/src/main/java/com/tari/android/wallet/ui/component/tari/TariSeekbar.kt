package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import com.tari.android.wallet.ui.common.domain.PaletteManager

class TariSeekbar(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    init {
        val brandColor = PaletteManager.getPurpleBrand(context)
        thumbTintList = ColorStateList.valueOf(brandColor)
        progressBackgroundTintList = ColorStateList.valueOf(brandColor)
        progressTintList = ColorStateList.valueOf(brandColor)
    }
}