package com.tari.android.wallet.ffi

class FFITariCoinPreview(pointer: FFIPointer) : FFIBase() {

    var vectorPointer: Long = -1
    var feeValue: Long = -1

    private external fun jniLoadData()

    init {
        this.pointer = pointer
        jniLoadData()
    }

    override fun destroy() = Unit
}