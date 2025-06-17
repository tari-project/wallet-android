package com.tari.android.wallet.ffi

class FFITariPaymentRecord(pointer: FFIPointer) : FFIBase() {

    var paymentReference: ByteArray? = null
    var amount: Long = -1L
    var blockHeight: Long = -1L
    var minedTimestamp: Long = -1L
    var direction: Int = -1

    private external fun jniLoadData()

    init {
        this.pointer = pointer
        jniLoadData()
    }

    override fun destroy() = Unit
}