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
package com.tari.android.wallet.ui.component.loadingSwitch

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.ViewProgressSwitchBinding
import com.tari.android.wallet.util.extension.setVisible


class TariLoadingSwitchView : FrameLayout {

    lateinit var ui: ViewProgressSwitchBinding

    private var isCheckedChangeListener: (isChecked: Boolean) -> Unit = { }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()

        setState(TariLoadingSwitchState(isChecked = false, isLoading = false))
    }

    private fun init() {
        ui = ViewProgressSwitchBinding.inflate(LayoutInflater.from(context), this, false).also { addView(it.root) }
    }

    fun setOnCheckedChangeListener(listener: (boolean: Boolean) -> Unit) {
        isCheckedChangeListener = listener
        ui.switchView.setOnCheckedChangeListener { _, isChecked -> isCheckedChangeListener.invoke(isChecked) }
    }

    fun setState(state: TariLoadingSwitchState) = with(ui) {
        if (switchView.isChecked != state.isChecked) {
            switchView.setOnCheckedChangeListener(null)
            switchView.isChecked = state.isChecked
            switchView.setOnCheckedChangeListener { _, isChecked -> isCheckedChangeListener.invoke(isChecked) }
        }

        switchView.setVisible(!state.isLoading)
        progressBar.setVisible(state.isLoading)
    }
}