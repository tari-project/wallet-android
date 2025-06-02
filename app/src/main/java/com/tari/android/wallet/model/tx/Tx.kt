/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.model.tx

import android.os.Parcelable
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import org.joda.time.DateTime
import java.math.BigInteger

/**
 * Base transaction class.
 *
 * @author The Tari Development Team
 */
abstract class Tx(
    open val id: TxId,
    open val direction: Direction,
    open val amount: MicroTari,
    open val timestamp: BigInteger, // Seconds
    open val paymentId: String?,
    open val status: TxStatus,
    open val tariContact: TariContact, // This is the receiver for an outbound tx and sender for an inbound tx.
) : Parcelable {

    enum class Direction {
        INBOUND,
        OUTBOUND
    }

    val isOneSided
        get() = status.isOneSided()

    val isCoinbase
        get() = status.isCoinbase()

    val isInbound: Boolean
        get() = direction == Direction.INBOUND
    val isOutbound: Boolean
        get() = direction == Direction.OUTBOUND

    val dateTime: DateTime
        get() = DateTime(timestamp.toLong() * 1000L)

    override fun toString() = "Tx(id=$id, " +
            "direction=$direction, " +
            "amount=$amount, " +
            "timestamp=$timestamp, " +
            "paymentId='$paymentId', " +
            "status=$status, " +
            "tariContact=$tariContact)"
}
