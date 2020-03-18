package com.tari.android.wallet.ffi

enum class FFIStatus {
    TX_NULL_ERROR,
    COMPLETED,
    BROADCAST,
    MINED,
    IMPORTED,
    PENDING,
    UNKNOWN
}