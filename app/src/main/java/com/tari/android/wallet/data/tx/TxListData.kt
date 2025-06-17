package com.tari.android.wallet.data.tx

import com.tari.android.wallet.model.TxStatus

data class TxListData(
    val cancelledTxs: List<TxDto> = emptyList(), // List<CancelledTx>
    val completedTxs: List<TxDto> = emptyList(), // List<CompletedTx>
    val pendingInboundTxs: List<TxDto> = emptyList(), // List<PendingInboundTx>
    val pendingOutboundTxs: List<TxDto> = emptyList(), // List<PendingOutboundTx>
) {
    val minedUnconfirmedTxs: List<TxDto>
        get() = completedTxs.filter { it.tx.status == TxStatus.MINED_UNCONFIRMED }
    val nonMinedUnconfirmedCompletedTxs: List<TxDto>
        get() = completedTxs.filter { it.tx.status != TxStatus.MINED_UNCONFIRMED }
    val pendingTxs: List<TxDto>
        get() = (pendingInboundTxs + pendingOutboundTxs + minedUnconfirmedTxs)
            .sortedWith(compareByDescending<TxDto> { it.tx.timestamp }.thenByDescending { it.tx.id })
    val nonPendingTxs: List<TxDto>
        get() = (cancelledTxs + nonMinedUnconfirmedCompletedTxs)
            .sortedWith(compareByDescending<TxDto> { it.tx.timestamp }.thenByDescending { it.tx.id })
    val allTxs: List<TxDto>
        get() = cancelledTxs + completedTxs + pendingInboundTxs + pendingOutboundTxs
}