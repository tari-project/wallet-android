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
package com.tari.android.wallet.ui.fragment.settings.logs.debugLog.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.databinding.ItemLogBinding
import com.tari.android.wallet.ffi.LogLevel
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible

class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val ui = ItemLogBinding.bind(view)

    fun bind(log: String) {
        ui.timestampTextView.gone()
        ui.source1TextView.gone()
        ui.source2TextView.gone()
        ui.levelTextView.gone()
        ui.logTextView.text = log.trim()
    }

    fun bind(timestamp: String?, source1: String?, source2: String?, level: String?, log: String?) {
        putTextInView(timestamp, ui.timestampTextView)
        putTextInView(source1, ui.source1TextView)
        putTextInView(source2, ui.source2TextView)

        putTextInView(level, ui.levelTextView)
        if (level != null && level.isNotEmpty()) {
            ui.levelTextView.setTextColor(LogLevel.from(level.trim()).color)
        }

        putTextInView(log, ui.logTextView)
    }

    private fun putTextInView(text: String?, view: TextView) {
        if (!text.isNullOrEmpty()) {
            view.visible()
            view.text = text.trim()
        } else {
            view.gone()
        }
    }
}
