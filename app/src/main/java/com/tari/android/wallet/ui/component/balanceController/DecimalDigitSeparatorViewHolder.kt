package com.tari.android.wallet.ui.component.balanceController

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewHomeBalanceDecimalSeparatorBinding
import com.tari.android.wallet.util.extension.dimenPx
import com.tari.android.wallet.util.extension.setTopMargin
import com.tari.android.wallet.util.extension.setWidthToMeasured
import com.tari.android.wallet.util.Constants

class DecimalDigitSeparatorViewHolder(context: Context, separator: String) : BalanceDigitViewHolder() {

    private val ui = ViewHomeBalanceDecimalSeparatorBinding.inflate(LayoutInflater.from(context))

    override val view: View
        get() = ui.root

    private val separatorTextView get() = ui.balanceDecimalSeparatorTextView

    init {
        separatorTextView.text = separator
        separatorTextView.setWidthToMeasured()
    }

    override fun reveal(delayMs: Long) {
        separatorTextView.setTopMargin(view.dimenPx(R.dimen.home_balance_digit_decimal_height))
        separatorTextView.setWidthToMeasured()
        animations += ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_decimal_height), 0).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                separatorTextView.setTopMargin(topMargin)
                separatorTextView.alpha = 1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_decimal_height).toFloat())
            }
            duration = Constants.UI.Home.digitAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            startDelay = delayMs
            start()
        }
    }
}