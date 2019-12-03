package com.tari.android.wallet.ffi
import android.util.Log

interface WalletListenerAdapter {
    fun onTransactionBroadcast(tx: CompletedTransaction) = Unit
    fun onTransactionMined(tx: CompletedTransaction) = Unit
    fun onTransactionReceived(tx: PendingInboundTransaction) = Unit
    fun onTransactionReplyReceived(tx: CompletedTransaction) = Unit
    fun onTransactionFinalized(tx : CompletedTransaction) = Unit
}