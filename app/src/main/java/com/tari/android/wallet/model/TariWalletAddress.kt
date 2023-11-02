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
import java.io.Serializable

/**
 * This wrapper is needed for id parameters in AIDL methods.
 *
 * @author The Tari Development Team
 */
class TariWalletAddress() : Parcelable, Serializable {

    var hexString = ""
    var emojiId = ""

    constructor(hexString: String, emojiId: String) : this() {
        // crunch fix for not crashing on action related to wallet address
        if (hexString == zeroHex) {
            this.hexString = zero66Hex
            this.emojiId =
                "\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF57"
        } else {
            this.hexString = hexString
            this.emojiId = emojiId
        }
    }

    fun isZeros(): Boolean = hexString == zeroHex || hexString == zero66Hex || hexString.all { it == '0' }

    override fun equals(other: Any?): Boolean = (other is TariWalletAddress) && hexString == other.hexString

    override fun hashCode(): Int = hexString.hashCode()

    override fun toString(): String = "TariWalletAddress(hexString='$hexString', emojiId='$emojiId')"

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<TariWalletAddress> {

        const val zeroHex = "0000000000000000000000000000000000000000000000000000000000000026"
        const val zero66Hex = "000000000000000000000000000000000000000000000000000000000000000026"

        override fun createFromParcel(parcel: Parcel): TariWalletAddress {
            return TariWalletAddress(parcel)
        }

        override fun newArray(size: Int): Array<TariWalletAddress> {
            return Array(size) { TariWalletAddress() }
        }

        fun validate(addressHex: String): Boolean = addressHex.length == 66
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(hexString)
        parcel.writeString(emojiId)
    }

    private fun readFromParcel(inParcel: Parcel) {
        hexString = inParcel.readString().orEmpty()
        emojiId = inParcel.readString().orEmpty()
    }

    override fun describeContents(): Int {
        return 0
    }

    // endregion
}
