package com.tari.android.wallet.ui.screen.tx.details.gif

import com.tari.android.wallet.model.*

data class TxState(val direction: Tx.Direction, val status: TxStatus) {
    companion object {
        fun from(tx: Tx): TxState {
            return when (tx) {
                is PendingInboundTx -> TxState(Tx.Direction.INBOUND, tx.status)
                is PendingOutboundTx -> TxState(Tx.Direction.OUTBOUND, tx.status)
                is CompletedTx -> TxState(tx.direction, tx.status)
                is CancelledTx -> TxState(tx.direction, tx.status)
                else -> throw IllegalArgumentException("Unexpected Tx type: $tx")
            }
        }
    }
}