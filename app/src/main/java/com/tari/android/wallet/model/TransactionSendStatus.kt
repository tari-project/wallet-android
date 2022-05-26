package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable

class TransactionSendStatus() : Parcelable {

    var status: Status = Status.Invalid

    constructor(result: Int) : this() {
        status = Status.findByInt(result)
    }

    constructor(parcel: Parcel) : this() {
        status = Status.findByInt(parcel.readInt())
    }

    val isDirectSend: Boolean
        get() = status in listOf(Status.DirectSend, Status.DirectSendSafSend)

    val isSafSend: Boolean
        get() = status in listOf(Status.DirectSendSafSend, Status.SafSend)

    val isQueued: Boolean
        get() = status == Status.Queued

    val isSuccess: Boolean
        get() = isDirectSend || isSafSend || !isQueued

    enum class Status(val value: Int) {
        Queued(0),
        DirectSendSafSend(1),
        DirectSend(2),
        SafSend(3),
        Invalid(4);

        companion object {
            fun findByInt(int: Int): Status = Status.values().first { it.value == int }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(status.value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionSendStatus> {
        override fun createFromParcel(parcel: Parcel): TransactionSendStatus = TransactionSendStatus(parcel)

        override fun newArray(size: Int): Array<TransactionSendStatus?> = arrayOfNulls(size)
    }
}