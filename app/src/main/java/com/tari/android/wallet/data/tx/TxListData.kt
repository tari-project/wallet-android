package com.tari.android.wallet.data.tx

import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingInboundTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.model.TxStatus

data class TxListData(
    val cancelledTxs: List<CancelledTx> = emptyList(),
    val completedTxs: List<CompletedTx> = emptyList(),
    val pendingInboundTxs: List<PendingInboundTx> = emptyList(),
    val pendingOutboundTxs: List<PendingOutboundTx> = emptyList(),
    val confirmationCount: Long, // TODO maybe not to pass to every instance, but to have a global value ?
) {
    val minedUnconfirmedTxs: List<CompletedTx>
        get() = completedTxs.filter { it.status == TxStatus.MINED_UNCONFIRMED }
    val nonMinedUnconfirmedCompletedTxs: List<CompletedTx>
        get() = completedTxs.filter { it.status != TxStatus.MINED_UNCONFIRMED }
    val allTxs: List<Tx>
        get() = cancelledTxs + completedTxs + pendingInboundTxs + pendingOutboundTxs
}