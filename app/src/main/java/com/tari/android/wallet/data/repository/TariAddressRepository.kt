package com.tari.android.wallet.data.repository

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject
import javax.inject.Singleton

// TODO remove this class and use TariWalletAddress constructors
@Singleton
class TariAddressRepository @Inject constructor() {

    /**
     * Try to find a valid Emoji ID or HEX in the given text and parse it to a [TariWalletAddress]. Returns null if no valid address is found.
     */
    fun parseValidWalletAddress(text: String): TariWalletAddress? {
        return text.trim().extractEmojis().windowed(size = Constants.Wallet.EMOJI_ID_LENGTH, step = 1)
            .map { it.joinToString("") }
            .firstNotNullOfOrNull { TariWalletAddress.fromEmojiIdOrNull(it) }
            ?: Regex("([A-Za-z0-9]{66})").findAll(text)
                .map { it.value }
                .mapNotNull { TariWalletAddress.fromBase58OrNull(it) }
                .firstOrNull()
    }
}