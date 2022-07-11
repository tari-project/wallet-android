package com.tari.android.wallet.ffi

class FFITariUtxo(pointer: FFIPointer) : FFIBase() {

    var commitment: String = ""
    var value: Long = -1
    var minedHeight: Long = -1
    var minedTimestamp: Long = -1
    var status: Byte = -1

    private external fun jniLoadData()

    init {
        this.pointer = pointer
        jniLoadData()
    }

    override fun destroy() = Unit
}