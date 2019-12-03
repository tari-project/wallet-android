package com.tari.android.wallet.ffi

class LibError {

    var code: Int = -1

    override fun toString(): String {
        return code.toString()
    }
}