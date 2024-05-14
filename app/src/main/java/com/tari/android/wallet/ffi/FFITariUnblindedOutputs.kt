package com.tari.android.wallet.ffi

class FFITariUnblindedOutputs() : FFIBase() {

    private external fun jniGetLength(libError: FFIError): Int
    private external fun jniGetAt(index: Int, libError: FFIError): FFIPointer
    private external fun jniDestroy()

    constructor(pointer: FFIPointer) : this() {
        this.pointer = pointer
    }

    fun getLength(): Int = runWithError { jniGetLength(it) }

    fun getAt(index: Int): FFITariUnblindedOutput = runWithError { FFITariUnblindedOutput(jniGetAt(index, it)) }

    override fun destroy() = jniDestroy()
}