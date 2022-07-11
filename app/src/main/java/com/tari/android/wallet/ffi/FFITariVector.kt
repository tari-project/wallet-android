package com.tari.android.wallet.ffi

class FFITariVector(pointer: FFIPointer) : FFIBase() {

    var len: Long = -1
    var cap: Long = -1
    var itemsList = mutableListOf<FFITariUtxo>()

    private external fun jniLoadData()

    private external fun jniGetItemAt(index: Int): FFIPointer

    init {
        this.pointer = pointer
        jniLoadData()
        for (i in 0 until len) {
            val utxoItem = FFITariUtxo(jniGetItemAt(i.toInt()))
            itemsList.add(utxoItem)
        }
    }

    override fun destroy() = Unit
}