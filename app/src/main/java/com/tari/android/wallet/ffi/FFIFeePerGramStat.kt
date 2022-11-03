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

    fun getOrder(): BigInteger = runWithError { BigInteger(1, jniGetOrder(it)) }

    fun getMin(): BigInteger = runWithError { BigInteger(1, jniGetMin(it)) }

    fun getMax(): BigInteger = runWithError { BigInteger(1, jniGetMax(it)) }

    fun getAverage(): BigInteger = runWithError { BigInteger(1, jniGetAverage(it)) }

    override fun destroy() = Unit
}