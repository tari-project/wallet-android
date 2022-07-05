package com.tari.android.wallet.ffi

enum class FFITariUtxoSort(val value: Int) {
    ValueAsc(0),
    ValueDesc(1),
    MinedHeightAsc(2),
    MinedHeightDesc(3),
}