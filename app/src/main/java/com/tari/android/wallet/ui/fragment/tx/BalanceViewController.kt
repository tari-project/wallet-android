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
package com.tari.android.wallet.ui.fragment.tx

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.HomeBalanceDecimalDigitBinding
import com.tari.android.wallet.databinding.HomeBalanceDecimalSeparatorBinding
import com.tari.android.wallet.databinding.HomeBalanceDigitBinding
import com.tari.android.wallet.databinding.HomeBalanceDigitThousandsSeparatorBinding
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setWidthToMeasured
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import java.lang.ref.WeakReference

/**
 * Controls the balance digit representation.
 *
 * @author The Tari Development Team
 */
internal class BalanceViewController(
    private val context: Context,
    private val digitContainerView: ViewGroup,
    decimalDigitContainerView: ViewGroup,
    private var _balanceInfo: BalanceInfo
) {

    private val viewHolders: ArrayList<BalanceDigitViewHolder> = ArrayList()
    private var formattedBalance: String

    private val delayByIndex: Long = 80L

    init {
        val balance = _balanceInfo.availableBalance + _balanceInfo.pendingIncomingBalance
        formattedBalance = WalletUtil.balanceFormatter.format(balance.tariValue)
        // decimal tens
        viewHolders.add(
            DecimalDigitViewHolder(
                context,
                formattedBalance[formattedBalance.length - 1].toString().toInt()
            )
        )
        decimalDigitContainerView.addView(viewHolders[0].view, 0)
        // decimal ones
        viewHolders.add(
            0,
            DecimalDigitViewHolder(
                context,
                formattedBalance[formattedBalance.length - 2].toString().toInt()
            )
        )
        decimalDigitContainerView.addView(viewHolders[0].view, 0)
        // decimal separator
        viewHolders.add(
            0,
            DecimalDigitSeparatorViewHolder(
                context,
                formattedBalance[formattedBalance.length - 3].toString()
            )
        )
        decimalDigitContainerView.addView(viewHolders[0].view, 0)

        // digits & separators
        for (i in (formattedBalance.length - 4) downTo 0 step 1) {
            if (formattedBalance[i].isDigit()) {
                viewHolders.add(
                    0,
                    DigitViewHolder(
                        context,
                        formattedBalance[i].toString().toInt()
                    )
                )
            } else {
                viewHolders.add(
                    0,
                    DigitSeparatorViewHolder(
                        context,
                        formattedBalance[i].toString()
                    )
                )
            }
            digitContainerView.addView(viewHolders[0].view, 0)
        }
    }

    fun runStartupAnimation() {
        // reveal views
        for ((index, holder) in viewHolders.iterator().withIndex()) {
            holder.reveal((index + 1) * delayByIndex)
        }
    }

    var balanceInfo: BalanceInfo
        get() = _balanceInfo
        set(value) {
            /* execute setter logic */
            _balanceInfo = value
            val balance = _balanceInfo.availableBalance + _balanceInfo.pendingIncomingBalance
            formattedBalance = WalletUtil.balanceFormatter.format(balance.tariValue)
            val sizeDiff = formattedBalance.length - viewHolders.size
            if (sizeDiff <= 0) {
                // delete items
                val wr = WeakReference(this)

                for (i in 0 until -sizeDiff) {
                    // remove animation
                    viewHolders[0].remove(i * delayByIndex, null)
                    viewHolders[0].shrink(
                        Constants.UI.Home.digitAnimDurationMs + (-sizeDiff - 1) * delayByIndex,
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                if (i == -sizeDiff - 1) {
                                    wr.get()?.digitContainerView?.removeViews(0, -sizeDiff)
                                }
                            }
                        }
                    )
                    viewHolders.removeAt(0)
                }

                // update items
                var changedItemCount = 0
                for ((index, digit) in formattedBalance.iterator().withIndex()) {
                    if (digit.isDigit()) {
                        val valueChanged = viewHolders[index].changeValue(
                            digit.toString().toInt(),
                            (delayByIndex * -sizeDiff) + changedItemCount * delayByIndex
                        )
                        if (valueChanged) {
                            changedItemCount++
                        }
                    }
                }

            } else {
                for (i in (sizeDiff - 1) downTo 0 step 1) {
                    if (formattedBalance[i].isDigit()) {
                        viewHolders.add(
                            0,
                            DigitViewHolder(
                                context,
                                formattedBalance[i].toString().toInt()
                            )
                        )
                    } else {
                        viewHolders.add(
                            0,
                            DigitSeparatorViewHolder(
                                context,
                                formattedBalance[i].toString()
                            )
                        )
                    }
                    digitContainerView.addView(viewHolders[0].view, 0)
                }

                for (i in 0 until sizeDiff) {
                    viewHolders[i].expand(0, null)
                    viewHolders[i].reveal(i * delayByIndex)
                }
                // update items
                var changedItemCount = 0
                for (i in sizeDiff until formattedBalance.length) {
                    if (formattedBalance[i].isDigit()) {
                        val valueChanged = viewHolders[i].changeValue(
                            formattedBalance[i].toString().toInt(),
                            (delayByIndex * sizeDiff) + changedItemCount * delayByIndex
                        )
                        if (valueChanged) {
                            changedItemCount++
                        }
                    }
                }
            }
        }

    abstract class BalanceDigitViewHolder {

        abstract val view: View

        abstract fun reveal(delayMs: Long)

        open fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            // default implementation :: noop
        }

        open fun expand(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            // default implementation :: noop
        }

        open fun shrink(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            val width = view.width
            val anim = ValueAnimator.ofFloat(1f, 0f)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                view.setLayoutWidth((width * animValue).toInt())
            }
            anim.duration = Constants.UI.Home.digitShrinkExpandAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.SINE_IN_OUT)
            anim.startDelay = delayMs
            if (animatorListener != null) {
                anim.addListener(animatorListener)
            }
            anim.start()
        }

        /**
         * Returns true if value has changed (a different value has been supplied).
         */
        open fun changeValue(newValue: Int, delayMs: Long): Boolean {
            // default implementation :: no-op
            return false
        }
    }

    class DigitSeparatorViewHolder(context: Context, separator: String) :
        BalanceDigitViewHolder() {

        private val ui =
            HomeBalanceDigitThousandsSeparatorBinding.inflate(LayoutInflater.from(context))

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
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                view.setLayoutWidth((width * animValue).toInt())
            }
            anim.duration = Constants.UI.Home.digitShrinkExpandAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.SINE_IN_OUT)
            anim.startDelay = delayMs
            if (animatorListener != null) {
                anim.addListener(animatorListener)
            }
            anim.start()
        }

        override fun reveal(delayMs: Long) {
            separatorTextView.setTopMargin(view.dimenPx(R.dimen.home_balance_digit_height))
            separatorTextView.setWidthToMeasured()
            val anim = ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_height), 0)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                separatorTextView.setTopMargin(topMargin)
                separatorTextView.alpha =
                    1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_height)
                        .toFloat())
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            anim.start()
        }

        override fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            val anim = ValueAnimator.ofFloat(1f, 0f)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                separatorTextView.alpha = animValue
                view.setTopMargin(
                    (view.dimenPx(R.dimen.home_balance_digit_height) * (1 - animValue)).toInt()
                )
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            if (animatorListener != null) {
                anim.addListener(animatorListener)
            }
            anim.start()
        }

    }

    class DigitViewHolder(context: Context, private var value: Int) : BalanceDigitViewHolder() {

        private var changed = false

        private val ui = HomeBalanceDigitBinding.inflate(LayoutInflater.from(context))

        override val view: View
            get() = ui.root

        init {
            ui.balanceTopDigitTextView.text = value.toString()
            ui.balanceTopDigitTextView.setWidthToMeasured()
        }

        override fun expand(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            val anim = ValueAnimator.ofFloat(0f, 1f)
            val width = ui.balanceTopDigitTextView.measuredWidth
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                view.setLayoutWidth((width * animValue).toInt())
            }
            anim.duration = Constants.UI.Home.digitShrinkExpandAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.SINE_IN_OUT)
            anim.startDelay = delayMs
            if (animatorListener != null) {
                anim.addListener(animatorListener)
            }
            anim.start()
        }

        override fun reveal(delayMs: Long) {
            ui.balanceTopDigitTextView.setTopMargin(
                view.dimenPx(R.dimen.home_balance_digit_height)
            )
            val anim = ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_height), 0)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                ui.balanceTopDigitTextView.setTopMargin(topMargin)
                ui.balanceTopDigitTextView.alpha =
                    1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_height)
                        .toFloat())
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            anim.start()
        }

        override fun changeValue(newValue: Int, delayMs: Long): Boolean {
            // no-op
            if (value == newValue) {
                return false
            }
            if (changed) {
                // reset
                ui.balanceTopDigitTextView.text = value.toString()
                ui.balanceTopDigitTextView.setWidthToMeasured()
                ui.balanceTopDigitTextView.alpha = 1f
                ui.balanceBottomDigitTextView.alpha = 0f
                ui.balanceTopDigitTextView.setTopMargin(0)
            }
            changed = true

            // update
            value = newValue
            ui.balanceBottomDigitTextView.text = value.toString()
            ui.balanceBottomDigitTextView.setWidthToMeasured()

            val anim = ValueAnimator.ofInt(0, -view.dimenPx(R.dimen.home_balance_digit_height) - 1)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                ui.balanceTopDigitTextView.setTopMargin(topMargin)
                val bottomAlpha =
                    -topMargin / view.dimenPx(R.dimen.home_balance_digit_height).toFloat()
                ui.balanceTopDigitTextView.alpha = 1 - bottomAlpha
                ui.balanceBottomDigitTextView.alpha = bottomAlpha
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.startDelay = delayMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.start()
            return true
        }

        override fun remove(delayMs: Long, animatorListener: Animator.AnimatorListener?) {
            if (changed) {
                // reset
                ui.balanceTopDigitTextView.text = value.toString()
                ui.balanceTopDigitTextView.setWidthToMeasured()
                ui.balanceTopDigitTextView.alpha = 1f
                ui.balanceBottomDigitTextView.alpha = 0f
                ui.balanceTopDigitTextView.setTopMargin(0)
            }
            changed = true

            // update
            val anim = ValueAnimator.ofFloat(1f, 0f)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                ui.balanceTopDigitTextView.alpha = animValue
                view.setTopMargin(
                    (view.dimenPx(R.dimen.home_balance_digit_height) * (1 - animValue)).toInt()
                )
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.startDelay = delayMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            if (animatorListener != null) {
                anim.addListener(animatorListener)
            }
            anim.start()
        }
    }

    class DecimalDigitSeparatorViewHolder(context: Context, separator: String) :
        BalanceDigitViewHolder() {

        private val ui =
            HomeBalanceDecimalSeparatorBinding.inflate(LayoutInflater.from(context))

        override val view: View
            get() = ui.root

        private val separatorTextView get() = ui.balanceDecimalSeparatorTextView

        init {
            separatorTextView.text = separator
            separatorTextView.setWidthToMeasured()
        }

        override fun reveal(delayMs: Long) {
            separatorTextView.setTopMargin(
                view.dimenPx(R.dimen.home_balance_digit_decimal_height)
            )
            separatorTextView.setWidthToMeasured()
            val anim =
                ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_decimal_height), 0)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                separatorTextView.setTopMargin(topMargin)
                separatorTextView.alpha =
                    1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_decimal_height)
                        .toFloat())
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            anim.start()
        }
    }

    class DecimalDigitViewHolder(context: Context, private var value: Int) :
        BalanceDigitViewHolder() {

        private var changed = false

        private val ui = HomeBalanceDecimalDigitBinding.inflate(LayoutInflater.from(context))

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
            topDecimalDigitTextView.setTopMargin(
                view.dimenPx(R.dimen.home_balance_digit_decimal_height)
            )
            val anim =
                ValueAnimator.ofInt(view.dimenPx(R.dimen.home_balance_digit_decimal_height), 0)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                topDecimalDigitTextView.setTopMargin(topMargin)
                topDecimalDigitTextView.alpha =
                    1 - (topMargin.toFloat() / view.dimenPx(R.dimen.home_balance_digit_decimal_height)
                        .toFloat())
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            anim.start()
        }

        override fun changeValue(newValue: Int, delayMs: Long): Boolean {
            // no-op
            if (value == newValue) {
                return false
            }
            if (changed) {
                // reset
                topDecimalDigitTextView.text = value.toString()
                topDecimalDigitTextView.setWidthToMeasured()
                topDecimalDigitTextView.alpha = 1f
                bottomDecimalDigitTextView.alpha = 0f
                topDecimalDigitTextView.setTopMargin(0)
            }
            changed = true

            // update
            value = newValue
            bottomDecimalDigitTextView.text = value.toString()
            bottomDecimalDigitTextView.setWidthToMeasured()
            val anim =
                ValueAnimator.ofInt(0, -view.dimenPx(R.dimen.home_balance_digit_decimal_height) - 1)
            anim.addUpdateListener { valueAnimator: ValueAnimator ->
                val topMargin = valueAnimator.animatedValue as Int
                topDecimalDigitTextView.setTopMargin(topMargin)
                val bottomAlpha =
                    -topMargin / view.dimenPx(R.dimen.home_balance_digit_decimal_height).toFloat()
                topDecimalDigitTextView.alpha = 1 - bottomAlpha
                bottomDecimalDigitTextView.alpha = bottomAlpha
            }
            anim.duration = Constants.UI.Home.digitAnimDurationMs
            anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            anim.startDelay = delayMs
            anim.start()
            return true
        }
    }

}
