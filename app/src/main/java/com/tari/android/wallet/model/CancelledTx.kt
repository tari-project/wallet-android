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
package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.extension.toMicroTari
import com.tari.android.wallet.ffi.FFICompletedTx
import com.tari.android.wallet.ffi.FFITxCancellationReason
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

/**
 * Canceled tx model class.
 *
 * @author The Tari Development Team
 */
@Parcelize
data class CancelledTx(
    override val id: TxId = 0.toBigInteger(),
    override val direction: Direction = Direction.INBOUND,
    override val amount: MicroTari = 0.toMicroTari(),
    override val timestamp: BigInteger = 0.toBigInteger(),
    override val message: String = "",
    override val paymentId: String = "",
    override val status: TxStatus = TxStatus.PENDING,
    override val tariContact: TariContact,
    val fee: MicroTari = 0.toMicroTari(),
    val cancellationReason: FFITxCancellationReason = FFITxCancellationReason.NotCancelled,
) : Tx(id, direction, amount, timestamp, message, paymentId, status, tariContact), Parcelable {

    constructor(tx: FFICompletedTx) : this(
        id = tx.getId(),
        direction = tx.getDirection(),
        tariContact = tx.getContact(),
        amount = MicroTari(tx.getAmount()),
        timestamp = tx.getTimestamp(),
        message = tx.getMessage(),
        paymentId = tx.getPaymentId(),
        status = TxStatus.map(tx.getStatus()),
        fee = MicroTari(tx.getFee()),
        cancellationReason = tx.getCancellationReason(),
    )

    override fun toString() = "CanceledTx(fee=$fee, status=$status) ${super.toString()}"
}