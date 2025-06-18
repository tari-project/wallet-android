package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.ffi.FFITariPaymentRecord
import com.tari.android.wallet.ffi.toHex
import kotlinx.parcelize.Parcelize

@Parcelize
data class TariPaymentRecord(
    val paymentReference: String,
    val amount: Long,
    val blockHeight: Long,
    val minedTimestamp: Long,
    val direction: Direction,
) : Parcelable {

    constructor(ffiObject: FFITariPaymentRecord) : this(
        paymentReference = ffiObject.paymentReference?.toHex() ?: "",
        amount = ffiObject.amount,
        blockHeight = ffiObject.blockHeight,
        minedTimestamp = ffiObject.minedTimestamp,
        direction = Direction.fromValue(ffiObject.direction),
    )

    enum class Direction(val value: Int) {
        Inbound(0),
        Outbound(1),
        Change(2);

        companion object {
            fun fromValue(value: Int): Direction = entries.first { it.value == value }
        }
    }
}