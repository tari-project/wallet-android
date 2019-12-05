package com.tari.android.wallet.ffi

interface OnTransactionMinedListener {

    fun onTransactionMined(ptr: CompletedTransactionPtr)

}