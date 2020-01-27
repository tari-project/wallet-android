/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
package com.tari.android.wallet.ui.activity.home

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.*
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.util.UiUtil
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.lang.RuntimeException
import java.lang.ref.WeakReference

/**
 * Recycler view adapter.
 *
 * @author The Tari Development Team
 */
internal class TxListRecyclerViewAdapter(
    private val completedTxs: List<CompletedTx>,
    private val pendingInboundTxs: List<PendingInboundTx>,
    private val pendingOutboundTxs: List<PendingOutboundTx>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val pendingHeaderViewType = 0
    private val headerViewType = 1
    private val txViewType = 2

    // items (headers and txs)
    private val items = ArrayList<Any>()
    private val pendingTxs = ArrayList<Tx>()

    fun notifyDataChanged() {
        items.clear()
        pendingTxs.clear()
        pendingTxs.addAll(pendingInboundTxs)
        pendingTxs.addAll(pendingOutboundTxs)

        var currentDate: LocalDate? = null
        // add pending txs
        if (pendingTxs.size > 0) {
            items.add(LocalDate.now())
            val sortedPendingTxs = ArrayList(pendingTxs)
                .sortedWith(compareByDescending<Tx> { it.timestamp }
                    .thenBy { it.contact.alias })
            items.addAll(sortedPendingTxs)
        }
        val sortedCompleteTxs = ArrayList(completedTxs)
            .sortedWith(compareByDescending<Tx> { it.timestamp }
                .thenBy { it.contact.alias })
        // completed txs
        for (tx in sortedCompleteTxs) {
            val txDate = DateTime(tx.timestamp.toLong() * 1000L).toLocalDate()
            if (currentDate == null || !txDate.isEqual(currentDate)) {
                currentDate = txDate
                items.add(currentDate)
            }
            items.add(tx)
        }
        notifyDataSetChanged()
    }

    /**
     * Item count.
     */
    override fun getItemCount() = items.size

    /**
     * Defines the view type - header or transaction.
     */
    override fun getItemViewType(position: Int): Int {
        return if (pendingTxs.size > 0 && position == 0) {
            pendingHeaderViewType
        } else if (items[position] is Tx) {
            txViewType
        } else {
            headerViewType
        }
    }

    /**
     * Create the view holder instance.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            pendingHeaderViewType -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_header, parent, false)
                HeaderViewHolder(
                    view,
                    HeaderViewHolder.Type.PENDING_TXS
                )
            }
            headerViewType -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_header, parent, false)
                HeaderViewHolder(
                    view,
                    HeaderViewHolder.Type.DATE
                )
            }
            txViewType -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_item, parent, false)
                TxViewHolder(
                    view
                )
            }
            else -> {
                throw RuntimeException("Unexpected view type $viewType.")
            }
        }
    }

    /**
     * Bind & display header or transaction.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            holder is TxViewHolder -> {
                holder.bind(items[position] as Tx)
            }
            getItemViewType(position) == headerViewType -> {
                (holder as HeaderViewHolder).bind(items[position] as LocalDate, position)
            }
            getItemViewType(position) == pendingHeaderViewType -> {
                (holder as HeaderViewHolder).bind(null, position)
            }
        }
    }

    /**
     * Section header view holder.
     */
    class HeaderViewHolder(view: View, private val type: Type) :
        RecyclerView.ViewHolder(view) {

        enum class Type {
            PENDING_TXS,
            DATE
        }

        private val dateFormat = "MMMM dd, yyyy"

        @BindView(R.id.home_tx_list_header_vw_separator)
        lateinit var separatorView: View
        @BindView(R.id.home_tx_list_header_txt_title)
        lateinit var titleTextView: TextView
        @BindView(R.id.home_tx_list_pending_prog_bar)
        lateinit var progressBar: ProgressBar


        @BindString(R.string.home_today)
        lateinit var todayString: String
        @BindString(R.string.home_yesterday)
        lateinit var yesterdayString: String
        @BindString(R.string.home_pending_txs)
        lateinit var pendingTxsString: String

        private var date: LocalDate? = null

        init {
            ButterKnife.bind(this, view)
        }

        fun bind(date: LocalDate?, position: Int) {
            if (position == 0) {
                separatorView.visibility = View.GONE
            } else {
                separatorView.visibility = View.VISIBLE
            }
            when (type) {
                Type.PENDING_TXS -> {
                    progressBar.visibility = View.VISIBLE
                    titleTextView.text = pendingTxsString
                }
                Type.DATE -> {
                    progressBar.visibility = View.GONE
                    this.date = date!!
                    val todayDate = LocalDate.now()
                    val yesterdayDate = todayDate.minusDays(1)
                    when {
                        date.isEqual(todayDate) -> {
                            titleTextView.text = todayString
                        }
                        date.isEqual(yesterdayDate) -> {
                            titleTextView.text = yesterdayString
                        }
                        else -> {
                            titleTextView.text = date.toString(dateFormat)
                        }
                    }
                }
            }

        }
    }

    /**
     * Transaction view holder class.
     *
     * @author The Tari Development Team
     */
    class TxViewHolder(view: View) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener {

        @BindView(R.id.home_tx_list_item_img_icon)
        lateinit var iconImageView: ImageView
        @BindView(R.id.home_tx_list_item_txt_contact_alias)
        lateinit var contactAliasTextView: TextView
        @BindView(R.id.home_tx_list_item_txt_message)
        lateinit var messageTextView: TextView
        @BindView(R.id.home_tx_list_item_txt_value)
        lateinit var valueTextView: TextView
        @BindDrawable(R.drawable.home_tx_value_positive_bg)
        lateinit var positiveBgDrawable: Drawable
        @BindColor(R.color.home_tx_value_positive)
        @JvmField
        var positiveColor: Int = 0
        @BindDrawable(R.drawable.home_tx_value_negative_bg)
        lateinit var negativeBgDrawable: Drawable
        @BindColor(R.color.home_tx_value_negative)
        @JvmField
        var negativeColor: Int = 0

        private lateinit var txWR: WeakReference<Tx>

        init {
            ButterKnife.bind(this, view)
        }

        @OnClick(R.id.home_tx_list_item_vw_root)
        override fun onClick(view: View) {
            UiUtil.temporarilyDisableClick(view)
            EventBus.post(Event.Home.TxClicked(txWR.get()!!))
        }

        fun bind(tx: Tx) {
            txWR = WeakReference(tx)
            // display contact alias
            contactAliasTextView.text = tx.contact.alias
            // display message
            messageTextView.text = tx.message
            // display value
            if (tx.direction == Tx.Direction.INBOUND) {
                val formattedValue = "+%1$,.2f".format(tx.amount.tariValue.toDouble())
                valueTextView.text = formattedValue
                valueTextView.setTextColor(positiveColor)
                valueTextView.background = positiveBgDrawable
            } else {
                val formattedValue = "-%1$,.2f".format(tx.amount.tariValue.toDouble())
                valueTextView.text = formattedValue
                valueTextView.setTextColor(negativeColor)
                valueTextView.background = negativeBgDrawable
            }
        }

    }

}