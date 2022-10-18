package com.tari.android.wallet.ffi

enum class TXOValidationStatus(val value: Int) {
    TxoValidationSuccess(0),
    TxoValidationAlreadyBusy(1),
    TxoValidationInternalFailure(2),
    TxoValidationCommunicationFailure(3)
}