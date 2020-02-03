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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.model.*
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.lang.RuntimeException
import java.lang.ref.WeakReference

/**
 * Transaction list recycler view adapter.
 *
 * @author The Tari Development Team
 */
internal class TxListAdapter(
    private val completedTxs: List<CompletedTx>,
    private val pendingInboundTxs: List<PendingInboundTx>,
    private val pendingOutboundTxs: List<PendingOutboundTx>,
    listener: Listener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    TxViewHolder.Listener {

    private val pendingHeaderViewType = 0
    private val headerViewType = 1
    private val txViewType = 2

    // items (headers and txs)
    private val items = ArrayList<Any>()
    private val pendingTxs = ArrayList<Tx>()

    /**
     * Listener.
     */
    private var listenerWR: WeakReference<Listener> = WeakReference(listener)

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
                .sortedWith(compareByDescending<Tx> { it.timestamp })
            items.addAll(sortedPendingTxs)
        }
        val sortedCompleteTxs = ArrayList(completedTxs)
            .sortedWith(compareByDescending<Tx> { it.timestamp })
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
                TxHeaderViewHolder(
                    view,
                    TxHeaderViewHolder.Type.PENDING_TXS
                )
            }
            headerViewType -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_header, parent, false)
                TxHeaderViewHolder(
                    view,
                    TxHeaderViewHolder.Type.DATE
                )
            }
            txViewType -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_item, parent, false)
                TxViewHolder(
                    view,
                    this
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
                (holder as TxHeaderViewHolder).bind(items[position] as LocalDate, position)
            }
            getItemViewType(position) == pendingHeaderViewType -> {
                (holder as TxHeaderViewHolder).bind(null, position)
            }
        }
    }

    override fun onTxSelected(tx: Tx) {
        listenerWR.get()?.onTxSelected(tx)
    }

    interface Listener {

        fun onTxSelected(tx: Tx)

    }

}