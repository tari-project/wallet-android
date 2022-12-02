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

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.ffi.FFICompletedTx
import com.tari.android.wallet.ffi.FFIPointer
import com.tari.android.wallet.ui.extension.readP
import com.tari.android.wallet.ui.extension.readS
import java.math.BigInteger

/**
 * Completed tx model class.
 *
 * @author The Tari Development Team
 */
class CompletedTx() : Tx(), Parcelable {

    var fee = MicroTari(BigInteger("0"))
    var confirmationCount = BigInteger("0")
    var txKernel: CompletedTransactionKernel? = null

    constructor(tx: FFICompletedTx) : this() {
        this.id = tx.getId()
        this.direction = tx.getDirection()
        this.user = tx.getUser()
        this.amount = MicroTari(tx.getAmount())
        this.fee = MicroTari(tx.getFee())
        this.timestamp = tx.getTimestamp()
        this.message = tx.getMessage()
        this.status = TxStatus.map(tx.getStatus())
        this.confirmationCount = tx.getConfirmationCount()
        if (this.status != TxStatus.IMPORTED && this.status != TxStatus.PENDING) {
            runCatching { tx.getTransactionKernel() }.getOrNull()?.let {
                this.txKernel = CompletedTransactionKernel(it.getExcess(), it.getExcessPublicNonce(), it.getExcessSignature())
            }
        }

        tx.destroy()
    }

    constructor(pointer: FFIPointer) : this(FFICompletedTx(pointer))


    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    override fun toString(): String {
        return "CompletedTx(fee=$fee, status=$status, confirmationCount=$confirmationCount) ${super.toString()}"
    }

    companion object CREATOR : Parcelable.Creator<CompletedTx> {

        override fun createFromParcel(parcel: Parcel): CompletedTx {
            return CompletedTx(parcel)
        }

        override fun newArray(size: Int): Array<CompletedTx> {
            return Array(size) { CompletedTx() }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(id)
        parcel.writeSerializable(direction)
        parcel.writeSerializable(user.javaClass)
        parcel.writeParcelable(user, flags)
        parcel.writeParcelable(amount, flags)
        parcel.writeParcelable(fee, flags)
        parcel.writeSerializable(timestamp)
        parcel.writeString(message)
        parcel.writeSerializable(status)
        parcel.writeSerializable(confirmationCount)
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readS(BigInteger::class.java)
        direction = inParcel.readS(Direction::class.java)
        val userIsContact = inParcel.readSerializable() == Contact::class.java
        user = if (userIsContact) {
            inParcel.readP(Contact::class.java)
        } else {
            inParcel.readP(User::class.java)
        }
        amount = inParcel.readP(MicroTari::class.java)
        fee = inParcel.readP(MicroTari::class.java)
        timestamp = inParcel.readS(BigInteger::class.java)
        message = inParcel.readString().orEmpty()
        status = inParcel.readS(TxStatus::class.java)
        confirmationCount = inParcel.readS(BigInteger::class.java)
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion
}