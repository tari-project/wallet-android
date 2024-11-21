package com.tari.android.wallet.data.recovery

import com.orhanobut.logger.Logger
import java.nio.ByteBuffer

/**
 * Represents the various states of the wallet restoration process.
 *
 * The flow of callbacks for a successful connection to a base node is as follows:
 *
 * - **ConnectingToBaseNode**: The process starts with a callback indicating that a connection attempt is in progress.
 *   This callback may be repeated multiple times until a connection is established.
 * - **ConnectedToBaseNode**: Once a connection is successfully made, this callback is triggered, indicating that the process has started.
 * - **Progress**: Progress callbacks will be in the form of (n, m) where n < m, showing the progress of the process.
 * - **Completed**: If the process completes successfully, this callback will be returned, indicating the number of UTXOs scanned and the amount of MicroTari recovered.
 * - **ConnectionToBaseNodeFailed**: If there is an error during the connection process, this callback will be returned.
 * - **ScanningRoundFailed**: If there is a minor error during scanning, this callback will be returned, and another connection/sync attempt will be made.
 * - **RecoveryFailed**: If an unrecoverable error occurs, this event will be returned, and the client will need to start a new process.
 */
sealed class WalletRestorationState {
    data object ConnectingToBaseNode : WalletRestorationState()
    data object ConnectedToBaseNode : WalletRestorationState()
    data class ConnectionToBaseNodeFailed(val retryCount: Long, val retryLimit: Long) : WalletRestorationState()
    data class Progress(val currentBlock: Long, val numberOfBlocks: Long) : WalletRestorationState()
    data class Completed(val numberOfUTXO: Long, val microTari: ByteArray) : WalletRestorationState()
    data class ScanningRoundFailed(val retryCount: Long, val retryLimit: Long) : WalletRestorationState()
    data object RecoveryFailed : WalletRestorationState()

    companion object {
        fun create(event: Int, firstArg: ByteArray, secondArgs: ByteArray): WalletRestorationState {
            val first = bytesToLong(firstArg)
            val second = bytesToLong(secondArgs)
            Logger.t("WalletRestorationResult $event $first $second")
            return when (event) {
                0 -> ConnectingToBaseNode
                1 -> ConnectedToBaseNode
                2 -> ConnectionToBaseNodeFailed(first, second)
                3 -> Progress(first, second)
                4 -> Completed(first, secondArgs)
                5 -> ScanningRoundFailed(first, second)
                6 -> RecoveryFailed
                else -> error("Invalid event type: $event")
            }
        }

        private fun bytesToLong(bytes: ByteArray): Long = ByteBuffer.allocate(java.lang.Long.BYTES).apply {
            put(bytes)
            flip()
        }.long
    }
}