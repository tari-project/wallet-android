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
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * This wrapper is needed for id parameters in AIDL methods.
 *
 * @author The Tari Development Team
 */
@Parcelize
data class TariWalletAddress(val hexString: String = "", val emojiId: String = "") : Parcelable, Serializable {

    fun isZeros(): Boolean = hexString == HEX_ZERO || hexString == HEX_ZERO_66 || hexString.all { it == '0' }

    override fun equals(other: Any?): Boolean = (other is TariWalletAddress) && hexString == other.hexString

    override fun hashCode(): Int = hexString.hashCode()

    override fun toString(): String = "TariWalletAddress(hexString='$hexString', emojiId='$emojiId')"

    companion object {
        const val HEX_ZERO = "0000000000000000000000000000000000000000000000000000000000000026"
        const val HEX_ZERO_66 = "000000000000000000000000000000000000000000000000000000000000000026"
        const val EMOJI_ZERO =
            "\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF00\uD83C\uDF57"

        fun createWalletAddress(hexString: String = HEX_ZERO, emojiId: String = ""): TariWalletAddress {
            // crunch fix for not crashing on action related to wallet address
            return if (hexString == HEX_ZERO) {
                TariWalletAddress(
                    hexString = HEX_ZERO_66,
                    emojiId = EMOJI_ZERO,
                )
            } else {
                TariWalletAddress(
                    hexString = hexString,
                    emojiId = emojiId,
                )
            }
        }

        fun validate(addressHex: String): Boolean = addressHex.length > 64
    }
}
