package com.tari.android.wallet.data.recovery

import com.orhanobut.logger.Logger
import java.nio.ByteBuffer

sealed class WalletRestorationState {

    data object NotStarted : WalletRestorationState()
    data class ScanningRoundFailed(val retryCount: Long, val retryLimit: Long) : WalletRestorationState()
    data class Progress(val currentBlock: Long, val numberOfBlocks: Long) : WalletRestorationState()
    data class Completed(val numberOfUTXO: Long, val microTari: ByteArray) : WalletRestorationState()


    companion object {
        fun create(event: Int, firstArg: ByteArray, secondArgs: ByteArray): WalletRestorationState {
            val first = bytesToLong(firstArg)
            val second = bytesToLong(secondArgs)
            Logger.t("WalletRestorationResult $event $first $second")
            return when (event) {
                0 -> Progress(first, second)
                1 -> Completed(first, secondArgs)
                2 -> ScanningRoundFailed(first, second)
                else -> error("Invalid event type: $event")
            }
        }

        private fun bytesToLong(bytes: ByteArray): Long = ByteBuffer.allocate(java.lang.Long.BYTES).apply {
            put(bytes)
            flip()
        }.long
    }
}