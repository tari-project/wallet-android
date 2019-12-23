package com.tari.android.wallet.ffi

import java.math.BigInteger
import java.util.*

class HexString constructor(bytes: ByteVector) {

    private val pattern = "\\p{XDigit}+".toRegex()

    var hex: String = String()

    init {
        if (bytes.getPointer() != nullptr) {
            val byteArray = ByteArray(bytes.getLength())
            if (bytes.getLength() > 0) {
                for (i in 0 until bytes.getLength()) {
                    val m = bytes.getAt(i)
                    byteArray[i] = m.toByte()
                }
                hex = BigInteger(1, byteArray) //
                    .toString(16) //a-f,0-9
                    .toUpperCase(Locale.getDefault()) //A-F are in lowercase in the final string before this call
            } else {
                hex = String()
            }
        } else {
            hex = String()
        }
    }

    constructor(string: String) : this(ByteVector(nullptr)) {
        if (pattern.matches(string) && string.length % 2 == 0) {
            hex = string
        } else {
            throw InvalidPropertiesFormatException("String is not valid Hex of even length")
        }
    }

    override fun toString(): String {
        return hex
    }


}