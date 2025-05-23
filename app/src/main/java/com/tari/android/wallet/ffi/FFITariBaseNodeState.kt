package com.tari.android.wallet.ffi

import java.math.BigInteger

class FFITariBaseNodeState() : FFIBase() {

    private external fun jniGetHeightOfLongestChain(libError: FFIError): ByteArray
    private external fun jniGetNodeId(libError: FFIError): FFIPointer

    constructor(pointer: FFIPointer) : this() {
        this.pointer = pointer
    }

    fun getHeightOfLongestChain(): BigInteger = runWithError { BigInteger(1, jniGetHeightOfLongestChain(it)) }

    fun getNodeId(): FFIByteVector? = runCatching { runWithError { FFIByteVector(jniGetNodeId(it)) } }.getOrNull()

    override fun destroy() { /* no-op, no method in FFI for destroy */
    }
}