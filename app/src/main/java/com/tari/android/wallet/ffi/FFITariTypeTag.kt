package com.tari.android.wallet.ffi

enum class FFITariTypeTag(val value: Int) {
    String(0),
    Utxo(1),
    Commitment(2),
}