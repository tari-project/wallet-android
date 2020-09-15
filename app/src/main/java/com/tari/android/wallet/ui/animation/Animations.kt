package com.tari.android.wallet.ui.animation

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setLayoutHeight

fun collapseAndHideAnimation(
    view: View,
    duration: Long = 300L,
    interpolator: TimeInterpolator = LinearInterpolator()
): ValueAnimator =
    ValueAnimator.ofInt(view.height, 0).apply {
        this.interpolator = interpolator
        this.duration = duration
        this.addUpdateListener { view.setLayoutHeight(it.animatedValue as Int) }
        this.addListener(onEnd = { view.gone() })
    }
