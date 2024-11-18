package com.tari.android.wallet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class TransactionSendStatus(
    val status: Status = Status.Invalid,
) : Parcelable {

    constructor(result: Int) : this(Status.findByInt(result))

    val isSuccess: Boolean
        get() = isDirectSend || isSafSend || !isQueued

    private val isDirectSend: Boolean
        get() = status in listOf(Status.DirectSend, Status.DirectSendSafSend)

    private val isSafSend: Boolean
        get() = status in listOf(Status.DirectSendSafSend, Status.SafSend)

    private val isQueued: Boolean
        get() = status == Status.Queued

    enum class Status(val value: Int) {
        Queued(0),
        DirectSendSafSend(1),
        DirectSend(2),
        SafSend(3),
        Invalid(4);

        companion object {
            fun findByInt(int: Int): Status = entries.first { it.value == int }
        }
    }
}