package com.tari.android.wallet.ffi

enum class FFITxCancellationReason {
    NotCancelled,
    Unknown,
    UserCancelled,
    Timeout,
    DoubleSpend,
    Orphan,
    TimeLocked,
    InvalidTransaction,
    AbandonedCoinbase;

    companion object {
        fun map(status: Int): FFITxCancellationReason {
            return when (status) {
                -1 -> NotCancelled
                0 -> Unknown
                1 -> UserCancelled
                2 -> Timeout
                3 -> DoubleSpend
                4 -> Orphan
                5 -> TimeLocked
                6 -> InvalidTransaction
                7 -> AbandonedCoinbase
                else -> throw FFIException(message = "Unexpected reason: $status")
            }
        }
    }
}