package com.tari.android.wallet.ffi

interface OnTransactionBroadcastListener {

    fun onTransactionBroadcast(ptr: CompletedTransactionPtr)

}