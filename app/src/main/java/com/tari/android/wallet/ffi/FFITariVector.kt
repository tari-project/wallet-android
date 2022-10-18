package com.tari.android.wallet.ffi

class FFITariVector(pointer: FFIPointer) : FFIBase() {

    var len: Long = -1
    var cap: Long = -1
    val tag: Int = -1
    var itemsList = mutableListOf<FFITariUtxo>()
    var longs = mutableListOf<Long>()

    private external fun jniLoadData()
    private external fun jniGetItemAt(index: Int): FFIPointer

    init {
        this.pointer = pointer
        jniLoadData()
        val vectorTag = TariVectorTag.values().firstOrNull { it.value == tag }
        for (i in 0 until len) {
            val newItemPointer = jniGetItemAt(i.toInt())
            when(vectorTag) {
                TariVectorTag.Utxo -> itemsList.add(FFITariUtxo(newItemPointer))
                TariVectorTag.U64 -> longs.add(newItemPointer)
                else -> Unit
            }
        }
    }

    override fun destroy() = Unit

    enum class TariVectorTag(val value: Int) {
        None(-1),
        Text(0),
        Utxo(1),
        Commitment(2),
        U64(3),
        I64(4),
    }
}