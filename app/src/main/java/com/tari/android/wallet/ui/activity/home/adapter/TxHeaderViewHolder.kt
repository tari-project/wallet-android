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
package com.tari.android.wallet.ui.activity.home.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindString
import butterknife.ButterKnife
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.HomeTxListHeaderBinding
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible
import org.joda.time.LocalDate

/**
 * Transaction list section header view holder.
 *
 * @author The Tari Development Team
 */
class TxHeaderViewHolder(view: View, private val type: Type) :
    RecyclerView.ViewHolder(view) {

    enum class Type {
        PENDING_TXS,
        DATE
    }

    private val dateFormat = "MMMM dd, yyyy"

    @BindString(R.string.home_today)
    lateinit var todayString: String

    @BindString(R.string.home_yesterday)
    lateinit var yesterdayString: String

    @BindString(R.string.home_pending_txs)
    lateinit var pendingTxsString: String

    private var date: LocalDate? = null

    private val ui = HomeTxListHeaderBinding.bind(view)

    init {
        ButterKnife.bind(this, view)
    }

    fun bind(date: LocalDate?, position: Int) {
        ui.txListHeaderSeparatorView.visibility = if (position == 0) View.GONE else View.VISIBLE
        when (type) {
            Type.PENDING_TXS -> {
                ui.txListHeaderPendingAnimationView.visible()
                ui.txListHeaderTitleTextView.text = pendingTxsString
            }
            Type.DATE -> {
                ui.txListHeaderPendingAnimationView.gone()
                this.date = date!!
                val todayDate = LocalDate.now()
                val yesterdayDate = todayDate.minusDays(1)
                ui.txListHeaderTitleTextView.text = when {
                    date.isEqual(todayDate) -> todayString
                    date.isEqual(yesterdayDate) -> yesterdayString
                    else -> date.toString(dateFormat)
                }
            }
        }

    }
}
