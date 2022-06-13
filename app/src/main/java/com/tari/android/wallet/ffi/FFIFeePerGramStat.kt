package com.tari.android.wallet.ffi

import java.math.BigInteger

class FFIFeePerGramStat(pointer: FFIPointer) : FFIBase() {

    private external fun jniGetOrder(libError: FFIError): ByteArray
    private external fun jniGetMin(libError: FFIError): ByteArray
    private external fun jniGetMax(libError: FFIError): ByteArray
    private external fun jniGetAverage(libError: FFIError): ByteArray


    init {
        this.pointer = pointer
    }

    fun getOrder(): BigInteger {
        val error = FFIError()
        val bytes = jniGetOrder(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getMin(): BigInteger {
        val error = FFIError()
        val bytes = jniGetMin(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getMax(): BigInteger {
        val error = FFIError()
        val bytes = jniGetMax(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getAverage(): BigInteger {
        val error = FFIError()
        val bytes = jniGetAverage(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    override fun destroy() = Unit
}