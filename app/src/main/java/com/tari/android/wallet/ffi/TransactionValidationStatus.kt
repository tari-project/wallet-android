package com.tari.android.wallet.ffi

enum class TransactionValidationStatus(val value: Int) {
    Success(0),
    AlreadyBusy(1),
    InternalFailure(2),
    CommunicationFailure(3)
}