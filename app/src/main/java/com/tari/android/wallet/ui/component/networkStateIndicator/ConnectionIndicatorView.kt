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
package com.tari.android.wallet.ui.component.networkStateIndicator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import com.tari.android.wallet.databinding.ViewConnectionIndicatorBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.component.tooltip.TooltipWindow
import com.tari.android.wallet.ui.extension.string


internal class ConnectionIndicatorView : CommonView<ConnectionIndicatorViewModel, ViewConnectionIndicatorBinding> {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean):
            ViewConnectionIndicatorBinding = ViewConnectionIndicatorBinding.inflate(layoutInflater, parent, attachToRoot)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var isDowned: Boolean = false
    private val tooltipWindow = TooltipWindow(context)

    override fun setup() = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_UP -> {
                if (isDowned && isExistPoint(event)) {
                    showTooltip()
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_DOWN -> {
                if (isExistPoint(event)) {
                    isDowned = true
                }
                false
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun bindViewModel(viewModel: ConnectionIndicatorViewModel) {
        super.bindViewModel(viewModel)

        with(viewModel) {
            observe(state) {
                ui.dot.setBackgroundResource(it.resId)

                if (tooltipWindow.isTooltipShown) {
                    tooltipWindow.dismissTooltip()
                    showTooltip()
                }
            }
        }
    }

    private fun showTooltip() {
        val message = string(viewModel.state.value!!.messageId)
        tooltipWindow.showToolTip(ui.root, message)
    }

    private fun isExistPoint(event: MotionEvent): Boolean {
        val screenPos = IntArray(2)
        this.getLocationOnScreen(screenPos)
        val rect = Rect(screenPos[0], screenPos[1], screenPos[0] + this.width, screenPos[1] + this.height)

        return rect.contains(event.rawX.toInt(), event.rawY.toInt())
    }
}

