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
import java.math.BigInteger

/**
 * Completed tx model class.
 *
 * @author The Tari Development Team
 */
class CompletedTx() : Tx(), Parcelable {

    var fee = MicroTari(BigInteger("0"))
    var status = Status.COMPLETED

    constructor(
        id: BigInteger,
        direction: Direction,
        user: User,
        amount: MicroTari,
        fee: MicroTari,
        timestamp: BigInteger,
        message: String,
        status: Status
    ) : this() {
        this.id = id
        this.direction = direction
        this.user = user
        this.amount = amount
        this.fee = fee
        this.timestamp = timestamp
        this.message = message
        this.status = status
    }

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
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
    }

    private fun readFromParcel(inParcel: Parcel) {
        id = inParcel.readSerializable() as BigInteger
        direction = inParcel.readSerializable() as Direction
        val userIsContact = inParcel.readSerializable() == Contact::class.java
        user = if (userIsContact) {
            inParcel.readParcelable(Contact::class.java.classLoader)!!
        } else {
            inParcel.readParcelable(User::class.java.classLoader)!!
        }
        amount = inParcel.readParcelable(MicroTari::class.java.classLoader)!!
        fee = inParcel.readParcelable(MicroTari::class.java.classLoader)!!
        timestamp = inParcel.readSerializable() as BigInteger
        message = inParcel.readString() ?: ""
        status = inParcel.readSerializable() as Status
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion

}