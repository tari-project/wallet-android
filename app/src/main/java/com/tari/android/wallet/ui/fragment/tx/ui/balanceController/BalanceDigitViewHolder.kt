package com.tari.android.wallet.ui.fragment.tx.ui.balanceController

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.util.Constants

abstract class BalanceDigitViewHolder {

    abstract val view: View

    abstract fun reveal(delayMs: Long)

    open fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) = Unit

    open fun expand(delayMs: Long, animatorListener: Animator.AnimatorListener?) = Unit

    open fun shrink(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
        val width = view.width
        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                view.setLayoutWidth((width * animValue).toInt())
            }
            duration = Constants.UI.Home.digitShrinkExpandAnimDurationMs
            interpolator = EasingInterpolator(Ease.SINE_IN_OUT)
            startDelay = delayMs
            if (animatorListener != null) {
                addListener(animatorListener)
            }
            start()
        }
    }

    /**
     * Returns true if value has changed (a different value has been supplied).
     */
    open fun changeValue(newValue: Int, delayMs: Long): Boolean = false
}