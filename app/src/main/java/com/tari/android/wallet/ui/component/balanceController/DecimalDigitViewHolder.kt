package com.tari.android.wallet.ui.component.balanceController

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewHomeBalanceDecimalDigitBinding
import com.tari.android.wallet.util.extension.dimenPx
import com.tari.android.wallet.util.extension.setTopMargin
import com.tari.android.wallet.util.extension.setWidthToMeasured
import com.tari.android.wallet.util.Constants

class DecimalDigitViewHolder(context: Context, private var value: Int) : BalanceDigitViewHolder() {

    private var changed = false

    private val ui = ViewHomeBalanceDecimalDigitBinding.inflate(LayoutInflater.from(context))

    init {
        topDecimalDigitTextView.text = value.toString()
        topDecimalDigitTextView.setWidthToMeasured()
    }

    override val view: View
        get() = ui.root

    private val topDecimalDigitTextView: TextView
        get() = ui.balanceTopDecimalDigitTextView

    private val bottomDecimalDigitTextView: TextView
        get() = ui.balanceBottomDecimalDigitTextView

    override fun reveal(delayMs: Long) {
        topDecimalDigitTextView.setTopMargin(view.dimenPx(R.dimen.home_balance_digit_decimal_height))
        animations += ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_decimal_height), 0).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                topDecimalDigitTextView.setTopMargin(topMargin)
                topDecimalDigitTextView.alpha = 1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_decimal_height).toFloat())
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
            topDecimalDigitTextView.text = value.toString()
            topDecimalDigitTextView.setWidthToMeasured()
            topDecimalDigitTextView.alpha = 1f
            bottomDecimalDigitTextView.alpha = 0f
            topDecimalDigitTextView.setTopMargin(0)
        }
        changed = true

        value = newValue
        bottomDecimalDigitTextView.text = value.toString()
        bottomDecimalDigitTextView.setWidthToMeasured()
        animations += ValueAnimator.ofInt(0, -view.dimenPx(R.dimen.home_balance_digit_decimal_height) - 1).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                topDecimalDigitTextView.setTopMargin(topMargin)
                val bottomAlpha = -topMargin / view.dimenPx(R.dimen.home_balance_digit_decimal_height).toFloat()
                topDecimalDigitTextView.alpha = 1 - bottomAlpha
                bottomDecimalDigitTextView.alpha = bottomAlpha
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            startDelay = delayMs
            start()
        }

        return true
    }
}