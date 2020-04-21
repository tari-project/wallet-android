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
package com.tari.android.wallet.ui.fragment.debug.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.databinding.LogItemBinding
import com.tari.android.wallet.ffi.LogLevel
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible

/**
 * Displays individual logs.
 *
 * @author The Tari Development Team
 */
class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val ui = LogItemBinding.bind(view)

    fun bind(
        log: String,
        isLast: Boolean = false
    ) {
        ui.timestampTextView.gone()
        ui.source1TextView.gone()
        ui.source2TextView.gone()
        ui.levelTextView.gone()
        ui.logTextView.text = log.trim()
        // bottom spacer
        ui.bottomSpacerView.visibility = when (isLast) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    fun bind(
        timestamp: String?,
        source1: String?,
        source2: String?,
        level: String?,
        log: String?,
        isLast: Boolean = false
    ) {
        // timestamp
        if (timestamp != null && timestamp.isNotEmpty()) {
            ui.timestampTextView.visible()
            ui.timestampTextView.text = timestamp.trim()
        } else {
            ui.timestampTextView.gone()
        }
        // source#1
        if (source1 != null && source1.isNotEmpty()) {
            ui.source1TextView.visible()
            ui.source1TextView.text = source1.trim()
        } else {
            ui.source1TextView.gone()
        }
        // source#2
        if (source2 != null && source2.isNotEmpty()) {
            ui.source2TextView.visible()
            ui.source2TextView.text = source2.trim()
        } else {
            ui.source2TextView.gone()
        }
        // level
        if (level != null && level.isNotEmpty()) {
            val logLevel = level.trim()
            ui.levelTextView.visible()
            ui.levelTextView.text = logLevel
            ui.levelTextView.setTextColor(
                LogLevel.from(logLevel).color
            )
        } else {
            ui.levelTextView.gone()
        }
        // log
        if (log != null && log.isNotEmpty()) {
            ui.logTextView.visible()
            ui.logTextView.text = log.trim()
        } else {
            ui.logTextView.gone()
        }
        // bottom spacer
        ui.bottomSpacerView.visibility = when (isLast) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

}
