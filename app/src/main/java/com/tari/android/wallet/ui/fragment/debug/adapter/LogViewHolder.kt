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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.tari.android.wallet.R
import com.tari.android.wallet.ffi.LogLevel
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible

/**
 * Displays individual logs.
 *
 * @author The Tari Development Team
 */
class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    @BindView(R.id.log_item_txt_timestamp)
    lateinit var timestampTextView: TextView
    @BindView(R.id.log_item_txt_source_1)
    lateinit var source1TextView: TextView
    @BindView(R.id.log_item_txt_source_2)
    lateinit var source2TextView: TextView
    @BindView(R.id.log_item_txt_level)
    lateinit var levelTextView: TextView
    @BindView(R.id.log_item_txt_log)
    lateinit var logTextView: TextView
    @BindView(R.id.log_item_vw_bottom_spacer)
    lateinit var bottomSpacer: View

    init {
        ButterKnife.bind(this, view)
    }

    fun bind(
        log: String,
        isLast: Boolean = false
    ) {
        timestampTextView.gone()
        source1TextView.gone()
        source2TextView.gone()
        levelTextView.gone()
        logTextView.text = log.trim()
        // bottom spacer
        bottomSpacer.visibility = when (isLast) {
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
            timestampTextView.visible()
            timestampTextView.text = timestamp.trim()
        } else {
            timestampTextView.gone()
        }
        // source#1
        if (source1 != null && source1.isNotEmpty()) {
            source1TextView.visible()
            source1TextView.text = source1.trim()
        } else {
            source1TextView.gone()
        }
        // source#2
        if (source2 != null && source2.isNotEmpty()) {
            source2TextView.visible()
            source2TextView.text = source2.trim()
        } else {
            source2TextView.gone()
        }
        // level
        if (level != null && level.isNotEmpty()) {
            val logLevel = level.trim()
            levelTextView.visible()
            levelTextView.text = logLevel
            levelTextView.setTextColor(
                LogLevel.from(logLevel).color
            )
        } else {
            levelTextView.gone()
        }
        // log
        if (log != null && log.isNotEmpty()) {
            logTextView.visible()
            logTextView.text = log.trim()
        } else {
            logTextView.gone()
        }
        // bottom spacer
        bottomSpacer.visibility = when (isLast) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

}