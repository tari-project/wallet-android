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
package com.tari.android.wallet.ui.component.balanceController

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.ViewGroup
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

/**
 * Controls the balance digit representation.
 *
 * @author The Tari Development Team
 */
class BalanceViewController(
    private val context: Context,
    private val digitContainerView: ViewGroup,
    decimalDigitContainerView: ViewGroup,
    private var _balanceInfo: BalanceInfo
) {

    private val viewHolders: ArrayList<BalanceDigitViewHolder> = ArrayList()
    private var formattedBalance: String

    private val delayByIndex: Long = 80L

    init {
        val balance = _balanceInfo.totalBalance
        formattedBalance = WalletConfig.balanceFormatter.format(balance.tariValue)
        // decimal tens
        viewHolders.add(DecimalDigitViewHolder(context, formattedBalance[formattedBalance.length - 1].toString().toInt()))
        decimalDigitContainerView.addView(viewHolders[0].view, 0)
        // decimal ones
        viewHolders.add(0, DecimalDigitViewHolder(context, formattedBalance[formattedBalance.length - 2].toString().toInt()))
        decimalDigitContainerView.addView(viewHolders[0].view, 0)
        // decimal separator
        viewHolders.add(0, DecimalDigitSeparatorViewHolder(context, formattedBalance[formattedBalance.length - 3].toString()))
        decimalDigitContainerView.addView(viewHolders[0].view, 0)

        // digits & separators
        for (i in (formattedBalance.length - 4) downTo 0 step 1) {
            if (formattedBalance[i].isDigit()) {
                viewHolders.add(0, DigitViewHolder(context, formattedBalance[i].toString().toInt()))
            } else {
                viewHolders.add(0, DigitSeparatorViewHolder(context, formattedBalance[i].toString()))
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
            val balance = _balanceInfo.totalBalance
            formattedBalance = WalletConfig.balanceFormatter.format(balance.tariValue)
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
                            override fun onAnimationEnd(animation: Animator) {
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
                        viewHolders.add(0, DigitViewHolder(context, formattedBalance[i].toString().toInt()))
                    } else {
                        viewHolders.add(0, DigitSeparatorViewHolder(context, formattedBalance[i].toString()))
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
}
