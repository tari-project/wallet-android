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
import com.tari.android.wallet.ffi.FFICompletedTx
import com.tari.android.wallet.ffi.FFIPendingOutboundTx
import com.tari.android.wallet.ffi.getPaymentIdSafely
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/**
 * Pending outbound tx model class.
 *
 * @author The Tari Development Team
 */
@Parcelize
data class PendingOutboundTx(
    override val id: TxId,
    override val direction: Direction,
    override val amount: MicroTari,
    override val timestamp: BigInteger,
    override val paymentId: String?,
    override val status: TxStatus,
    override val tariContact: TariContact,
    val fee: MicroTari,
) : Tx(id, direction, amount, timestamp, paymentId, status, tariContact), Parcelable {

    constructor(tx: FFICompletedTx) : this(
        id = tx.getId(),
        direction = tx.getDirection(),
        tariContact = tx.getContact(),
        amount = tx.getAmount().toMicroTari(),
        fee = tx.getFee().toMicroTari(),
        timestamp = tx.getTimestamp(),
        paymentId = tx.getPaymentIdSafely(),
        status = TxStatus.map(tx.getStatus()),
    )

    constructor(tx: FFIPendingOutboundTx) : this(
        id = tx.getId(),
        direction = tx.getDirection(),
        tariContact = tx.getContact(),
        amount = MicroTari(tx.getAmount()),
        fee = MicroTari(tx.getFee()),
        timestamp = tx.getTimestamp(),
        paymentId = tx.getPaymentIdSafely(),
        status = TxStatus.map(tx.getStatus()),
    )

    override fun toString() = "PendingOutboundTx(fee=$fee, status=$status) ${super.toString()}"

    override val rawDetails: String
        get() = "{" +
                "\"id\":\"$id\"," +
                "\"direction\":\"$direction\"," +
                "\"amount\":\"${amount.value}\"," +
                "\"fee\":\"${fee.value}\"," +
                "\"timestamp\":\"$timestamp\"," +
                "\"paymentId\":\"$paymentId\"," +
                "\"status\":\"$status\"," +
                "}"
}
