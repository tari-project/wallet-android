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
package com.tari.android.wallet.ui.fragment.tx.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.model.*

/**
 * Transaction list recycler view adapter.
 *
 * @author The Tari Development Team
 */
internal class TxListAdapter(
    private val cancelledTxs: List<CancelledTx>,
    private val completedTxs: List<CompletedTx>,
    private val pendingInboundTxs: List<PendingInboundTx>,
    private val pendingOutboundTxs: List<PendingOutboundTx>,
    private val listener: (Tx) -> Unit
) : RecyclerView.Adapter<TxViewHolder>() {

    // transactions
    private val items = ArrayList<Tx>()

    fun notifyDataChanged() {
        items.clear()
        // sort and add pending txs
        val pendingTxs = (pendingInboundTxs + pendingOutboundTxs).toMutableList()
        pendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        items.addAll(pendingTxs)
        // sort and add non-pending txs
        val nonPendingTxs = (cancelledTxs + completedTxs).toMutableList()
        nonPendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        items.addAll(nonPendingTxs)
        // update UI
        super.notifyDataSetChanged()
    }

    /**
     * Item count.
     */
    override fun getItemCount() = items.size


    /**
     * Create the view holder instance.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TxViewHolder =
        TxViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.home_tx_list_item, parent, false),
            listener
        )

    /**
     * Bind & display header or transaction.
     */
    override fun onBindViewHolder(holder: TxViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun onViewAttachedToWindow(holder: TxViewHolder) {
        holder.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: TxViewHolder) {
        holder.onDetach()
    }

}
