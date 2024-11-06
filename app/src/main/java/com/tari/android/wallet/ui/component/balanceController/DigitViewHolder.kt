package com.tari.android.wallet.ui.component.balanceController

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewHomeBalanceDigitBinding
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setWidthToMeasured
import com.tari.android.wallet.util.Constants

class DigitViewHolder(context: Context, private var value: Int) : BalanceDigitViewHolder() {

    private var changed = false

    private val ui = ViewHomeBalanceDigitBinding.inflate(LayoutInflater.from(context))

    override val view: View
        get() = ui.root

    init {
        ui.balanceTopDigitTextView.text = value.toString()
        ui.balanceTopDigitTextView.setWidthToMeasured()
    }

    override fun expand(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
        val width = ui.balanceTopDigitTextView.measuredWidth
        ValueAnimator.ofFloat(0f, 1f).apply {
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
        ui.balanceTopDigitTextView.setTopMargin(view.dimenPx(R.dimen.home_balance_digit_height))
        ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_height), 0).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                ui.balanceTopDigitTextView.setTopMargin(topMargin)
                ui.balanceTopDigitTextView.alpha = 1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_height).toFloat())
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            startDelay = delayMs
            start()
        }
    }

    override fun changeValue(newValue: Int, delayMs: Long): Boolean {
        if (value == newValue) {
            return false
        }
        if (changed) {
            ui.balanceTopDigitTextView.text = value.toString()
            ui.balanceTopDigitTextView.setWidthToMeasured()
            ui.balanceTopDigitTextView.alpha = 1f
            ui.balanceBottomDigitTextView.alpha = 0f
            ui.balanceTopDigitTextView.setTopMargin(0)
        }
        changed = true

        value = newValue
        ui.balanceBottomDigitTextView.text = value.toString()
        ui.balanceBottomDigitTextView.setWidthToMeasured()

        ValueAnimator.ofInt(0, -view.dimenPx(R.dimen.home_balance_digit_height) - 1).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                ui.balanceTopDigitTextView.setTopMargin(topMargin)
                val bottomAlpha = -topMargin / view.dimenPx(R.dimen.home_balance_digit_height).toFloat()
                ui.balanceTopDigitTextView.alpha = 1 - bottomAlpha
                ui.balanceBottomDigitTextView.alpha = bottomAlpha
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            startDelay = delayMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            start()
        }

        return true
    }

    override fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
        if (changed) {
            ui.balanceTopDigitTextView.text = value.toString()
            ui.balanceTopDigitTextView.setWidthToMeasured()
            ui.balanceTopDigitTextView.alpha = 1f
            ui.balanceBottomDigitTextView.alpha = 0f
            ui.balanceTopDigitTextView.setTopMargin(0)
        }
        changed = true

        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                ui.balanceTopDigitTextView.alpha = animValue
                view.setTopMargin(
                    (view.dimenPx(R.dimen.home_balance_digit_height) * (1 - animValue)).toInt()
                )
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            startDelay = delayMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            if (animatorListener != null) {
                addListener(animatorListener)
            }
            start()
        }
    }
}