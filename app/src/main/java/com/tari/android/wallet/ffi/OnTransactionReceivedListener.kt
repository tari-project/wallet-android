package com.tari.android.wallet.ffi

interface OnTransactionReceivedListener {

    fun onTransactionReceived(ptr: PendingInboundTransactionPtr)

}