package com.tari.android.wallet.ui.component.balanceController

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewHomeBalanceDigitThousandsSeparatorBinding
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setWidthToMeasured
import com.tari.android.wallet.util.Constants

class DigitSeparatorViewHolder(context: Context, separator: String) : BalanceDigitViewHolder() {

    private val ui = ViewHomeBalanceDigitThousandsSeparatorBinding.inflate(LayoutInflater.from(context))

    init {
        separatorTextView.text = separator
        separatorTextView.setWidthToMeasured()
    }

    override val view: View
        get() = ui.root

    private val separatorTextView
        get() = ui.balanceThousandsSeparatorTextView

    override fun expand(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
        val width = separatorTextView.measuredWidth
        animations += ValueAnimator.ofFloat(0f, 1f).apply {
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

    override fun reveal(delayMs: Long) {
        separatorTextView.setTopMargin(view.dimenPx(R.dimen.home_balance_digit_height))
        separatorTextView.setWidthToMeasured()
        animations += ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_height), 0).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                separatorTextView.setTopMargin(topMargin)
                separatorTextView.alpha = 1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_height).toFloat())
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            startDelay = delayMs
            start()
        }
    }

    override fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
        animations += ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                separatorTextView.alpha = animValue
                view.setTopMargin((view.dimenPx(R.dimen.home_balance_digit_height) * (1 - animValue)).toInt())
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            startDelay = delayMs
            if (animatorListener != null) {
                addListener(animatorListener)
            }
            start()
        }
    }
}