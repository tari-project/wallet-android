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
import com.tari.android.wallet.extension.flag
import com.tari.android.wallet.ffi.Base58String
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.util.tariEmoji
import kotlinx.parcelize.Parcelize

/**
 * This wrapper is needed for id parameters in AIDL methods.
 *
 * @author The Tari Development Team
 */
@Parcelize
data class TariWalletAddress(
    val network: Network,
    val features: List<Feature>,

    val networkEmoji: EmojiId,
    val featuresEmoji: EmojiId,
    val viewKeyEmojis: EmojiId?,
    val spendKeyEmojis: EmojiId,
    val checksumEmoji: EmojiId,

    val fullBase58: Base58,
    val fullEmojiId: EmojiId,

    val unknownAddress: Boolean, // true for one-sided payment or phone contact
) : Parcelable {

    constructor(ffiWalletAddress: FFITariWalletAddress) : this(
        network = Network.get(ffiWalletAddress.getNetwork()),
        features = Feature.get(ffiWalletAddress.getFeatures()),
        networkEmoji = ffiWalletAddress.getNetwork().tariEmoji(),
        featuresEmoji = ffiWalletAddress.getFeatures().tariEmoji(),
        viewKeyEmojis = ffiWalletAddress.getViewKey()?.getEmojiId(),
        spendKeyEmojis = ffiWalletAddress.getSpendKey().getEmojiId(),
        checksumEmoji = ffiWalletAddress.getChecksum().tariEmoji(),
        fullBase58 = ffiWalletAddress.fullBase58(),
        fullEmojiId = ffiWalletAddress.getEmojiId(),
        unknownAddress = ffiWalletAddress.getSpendKey().getByteVector().byteArray().all { it == 0.toByte() },
    )

    val uniqueIdentifier: String
        get() = "$networkEmoji$spendKeyEmojis"

    val coreKeyEmojis: EmojiId
        get() = viewKeyEmojis + spendKeyEmojis + checksumEmoji

    val oneSided: Boolean
        get() = features.contains(Feature.ONE_SIDED)

    val interactive: Boolean
        get() = features.contains(Feature.INTERACTIVE)

    fun isUnknownUser(): Boolean = unknownAddress

    override fun equals(other: Any?): Boolean = (other is TariWalletAddress) && uniqueIdentifier == other.uniqueIdentifier

    override fun hashCode(): Int = uniqueIdentifier.hashCode()

    override fun toString(): String = "TariWalletAddress(base58='$fullBase58', emojiId='$fullEmojiId')"

    companion object {

        @Throws(FFIException::class)
        fun fromBase58(base58: Base58) = FFITariWalletAddress(base58 = Base58String(base58)).runWithDestroy { TariWalletAddress(it) }

        @Throws(FFIException::class)
        fun fromEmojiId(emojiId: EmojiId) = FFITariWalletAddress(emojiId = emojiId).runWithDestroy { TariWalletAddress(it) }

        fun fromBase58OrNull(base58: Base58): TariWalletAddress? = runCatching { fromBase58(base58) }.getOrNull()

        fun fromEmojiIdOrNull(emojiId: EmojiId): TariWalletAddress? = runCatching { fromEmojiId(emojiId) }.getOrNull()

        /**
         * Tries to create a TariWalletAddress from a base58 string or an emoji id.
         */
        @Throws(FFIException::class)
        fun makeTariAddress(input: String): TariWalletAddress = runCatching { fromBase58(input) }.recoverCatching { fromEmojiId(input) }.getOrThrow()

        fun makeTariAddressOrNull(input: String): TariWalletAddress? = runCatching { makeTariAddress(input) }.getOrNull()

        fun validateBase58(base58: Base58): Boolean = fromBase58OrNull(base58) != null

        fun validateEmojiId(emojiId: EmojiId): Boolean = fromEmojiIdOrNull(emojiId) != null
    }

    enum class Network {
        MAINNET,
        STAGENET,
        NEXTNET,
        TESTNET;

        companion object {
            fun get(value: Int) = when (value) {
                0 -> MAINNET
                1 -> STAGENET
                2 -> NEXTNET
                else -> TESTNET
            }
        }
    }

    enum class Feature(val mask: Byte) {
        ONE_SIDED(0b00000001),
        INTERACTIVE(0b00000010);

        companion object {
            fun get(features: Int): List<Feature> = entries.filter { features.toByte().flag(it.mask) }
        }
    }
}

fun FFITariWalletAddress.fullBase58(): Base58 = listOf(
    Base58String(this.getNetwork().toByte()).base58,
    Base58String(this.getFeatures().toByte()).base58,
    Base58String(this.getByteVector().byteArray().drop(2)).base58,
).joinToString(separator = "")