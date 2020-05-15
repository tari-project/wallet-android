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

/**
 * Transaction list recycler view adapter.
 *
 * @author The Tari Development Team
 */
// TODO consider using diffutil
internal class TxListAdapter(
    private val cancelledTxes: List<CancelledTx>,
    private val completedTxs: List<CompletedTx>,
    private val pendingInboundTxs: List<PendingInboundTx>,
    private val pendingOutboundTxs: List<PendingOutboundTx>,
    private val listener: (Tx) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val pendingHeaderViewType = 0
    private val headerViewType = 1
    private val txViewType = 2

    // items (headers and txs)
    private val items = ArrayList<Any>()
    private var hasPendingTxs = false

    fun notifyDataChanged() {
        items.clear()
        val pendingTxs = ArrayList<Tx>(pendingInboundTxs.size + pendingOutboundTxs.size).apply {
            addAll(pendingInboundTxs)
            addAll(pendingOutboundTxs)
        }
        hasPendingTxs = pendingTxs.isNotEmpty()
        // add pending txs
        if (hasPendingTxs) {
            items.add(LocalDate.now())
            items.addAll(pendingTxs.sortedWith(compareByDescending(Tx::timestamp)))
        }
        val sortedFinishedTxs =
            ArrayList<Tx>(cancelledTxes.size + completedTxs.size).apply {
                addAll(cancelledTxes)
                addAll(completedTxs)
            }.sortedWith(compareByDescending { it.timestamp })
        // completed & canceled txs
        var currentDate: LocalDate? = null
        for (tx in sortedFinishedTxs) {
            val txDate = DateTime(tx.timestamp.toLong() * 1000L).toLocalDate()
            if (currentDate == null || !txDate.isEqual(currentDate)) {
                currentDate = txDate
                items.add(currentDate)
            }
            items.add(tx)
        }
        super.notifyDataSetChanged()
    }

    /**
     * Item count.
     */
    override fun getItemCount() = items.size

    /**
     * Defines the view type - header or transaction.
     */
    override fun getItemViewType(position: Int): Int = when {
        position == 0 && hasPendingTxs -> pendingHeaderViewType
        items[position] is Tx -> txViewType
        else -> headerViewType
    }

    /**
     * Create the view holder instance.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder =
    // TODO [DISCUSS] adjust margin at the point of instantiation and have a
        // single textview in the xml?
        when (viewType) {
            pendingHeaderViewType -> TxHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_header, parent, false),
                TxHeaderViewHolder.Type.PENDING_TXS
            )
            headerViewType -> TxHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_header, parent, false),
                TxHeaderViewHolder.Type.DATE
            )
            txViewType -> TxViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_tx_list_item, parent, false), listener
            )
            else -> throw RuntimeException("Unexpected view type $viewType.")
        }

    /**
     * Bind & display header or transaction.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            holder is TxViewHolder -> holder.bind(items[position] as Tx)
            holder.itemViewType == headerViewType ->
                (holder as TxHeaderViewHolder).bind(items[position] as LocalDate, position)
            holder.itemViewType == pendingHeaderViewType ->
                (holder as TxHeaderViewHolder).bind(null, position)
        }
    }

}
