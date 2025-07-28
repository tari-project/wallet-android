package com.tari.android.wallet.ffi

import java.math.BigInteger

class FFITariBaseNodeState() : FFIBase() {

    private external fun jniGetHeightOfLongestChain(libError: FFIError): ByteArray

    constructor(pointer: FFIPointer) : this() {
        this.pointer = pointer
    }

    fun getHeightOfLongestChain(): BigInteger = runWithError { BigInteger(1, jniGetHeightOfLongestChain(it)) }

    override fun destroy() { /* no-op, no method in FFI for destroy */
    }
}