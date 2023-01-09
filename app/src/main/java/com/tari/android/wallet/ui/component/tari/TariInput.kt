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
package com.tari.android.wallet.ui.component.tari

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.TariInputBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.obtain
import com.tari.android.wallet.ui.extension.runRecycle
import com.tari.android.wallet.ui.extension.setVisible

class TariInput(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    val ui: TariInputBinding
    val paletteManager = PaletteManager()

    var textChangedListener: (text: CharSequence?, start: Int, before: Int, count: Int) -> Unit = { _, _, _, _ -> }

    init {
        ui = TariInputBinding.inflate(LayoutInflater.from(context), this, false).also { addView(it.root) }

        setErrorText(null)
        setIsInvalid(false)
        setText("")

        obtain(attrs, R.styleable.TariInput).runRecycle {
            getString(R.styleable.TariInput_hintText)?.let { ui.editText.hint = it }
            setErrorText(getString(R.styleable.TariInput_errorText))
            if (getBoolean(R.styleable.TariInput_android_singleLine, true)) ui.editText.setSingleLine()
            ui.editText.imeOptions = getInt(R.styleable.TariInput_android_imeOptions, 0)
            ui.editText.inputType = getInt(R.styleable.TariInput_android_inputType, 0)
            ui.editText.isEnabled = getBoolean(R.styleable.TariInput_android_enabled, true)
        }

        ui.editText.addTextChangedListener(onTextChanged = { text, start, before, count -> textChangedListener.invoke(text, start, before, count) })
    }

    override fun isEnabled(): Boolean = ui.editText.isEnabled
    override fun setEnabled(enabled: Boolean) {
        ui.editText.isEnabled = enabled
    }

    fun setText(text: String) = ui.editText.setText(text)

    fun setIsInvalid(isInvalid: Boolean) {
        ui.invalidMessage.setVisible(isInvalid)
        val backColor = if (isInvalid) paletteManager.getRed(context) else paletteManager.getNeutralSecondary(context)
        ui.divider.setBackgroundColor(backColor)
    }

    fun setErrorText(errorText: String?) {
        ui.invalidMessage.text = errorText.orEmpty()
    }
}