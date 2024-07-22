package com.tari.android.wallet.data.repository

import com.tari.android.wallet.extension.getResultWithError
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject
import javax.inject.Singleton

// TODO remove this class and use TariWalletAddress constructors
@Singleton
class TariAddressRepository @Inject constructor(
    private val serviceConnection: TariWalletServiceConnection,
) {

    suspend fun walletAddressFromHex(hex: String): Result<TariWalletAddress> {
        return serviceConnection.doOnWalletServiceConnectedWithValue { walletService ->
            walletService.walletAddressFromHex(hex)
        }
    }

    suspend fun walletAddressFromEmojiId(emojiId: String): Result<TariWalletAddress> {
        return serviceConnection.doOnWalletServiceConnectedWithValue { walletService ->
            walletService.getResultWithError { error, service -> service.getWalletAddressFromEmojiId(emojiId, error) }
        }
    }

    /**
     * Try to find a valid Emoji ID or HEX in the given text and parse it to a [TariWalletAddress]. Returns null if no valid address is found.
     */
    fun parseValidWalletAddress(text: String): TariWalletAddress? {
        return text.trim().extractEmojis().windowed(size = Constants.Wallet.emojiIdLength, step = 1)
            .map { it.joinToString("") }
            .firstNotNullOfOrNull { TariWalletAddress.fromEmojiIdOrNull(it) }
            ?: Regex("([A-Za-z0-9]{66})").findAll(text)
                .map { it.value }
                .mapNotNull { TariWalletAddress.fromBase58OrNull(it) }
                .firstOrNull()
    }

    private fun TariWalletService.walletAddressFromHex(hex: String): Result<TariWalletAddress> =
        this.getResultWithError { error, service -> service.getWalletAddressFromBase58(hex, error) }

    private fun TariWalletService.walletAddressFromEmojiId(emojiId: String): Result<TariWalletAddress> =
        this.getResultWithError { error, service -> service.getWalletAddressFromEmojiId(emojiId, error) }

    companion object {
        fun validateHex(addressHex: String): Boolean = addressHex.length > 64
    }
}