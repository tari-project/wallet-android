package com.tari.android.wallet.ffi

class FFITariPaymentRecords() : FFIIterableBase<FFITariPaymentRecord>() {

    private external fun jniGetLength(libError: FFIError): Int
    private external fun jniGetAt(index: Int, libError: FFIError): FFIPointer
    private external fun jniDestroy()

    constructor(pointer: FFIPointer) : this() {
        if (pointer.isNull()) error("Pointer must not be null")
        this.pointer = pointer
    }

    override fun getLength(): Int = runWithError { jniGetLength(it) }

    override fun getAt(index: Int): FFITariPaymentRecord = runWithError { FFITariPaymentRecord(jniGetAt(index, it)) }

    override fun destroy() = jniDestroy()
}