package com.tari.android.wallet.ffi

interface OnTransactionReplyReceivedListener {

    fun onTransactionReplyReceived(ptr: CompletedTransactionPtr)

}