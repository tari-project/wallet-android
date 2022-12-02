package com.tari.android.wallet.model.recovery

import com.orhanobut.logger.Logger
import java.nio.ByteBuffer

// If connection to a base node is successful the flow of callbacks should be:
//     - The process will start with a callback with `ConnectingToBaseNode` showing a connection is being attempted
//       this could be repeated multiple times until a connection is made.
//     - The next a callback with `ConnectedToBaseNode` indicate a successful base node connection and process has
//       started
//     - In Progress callbacks will be of the form (n, m) where n < m
//     - If the process completed successfully then the final `Completed` callback will return how many UTXO's were
//       scanned and how much MicroTari was recovered
//     - If there is an error in the connection process then the `ConnectionToBaseNodeFailed` will be returned
//     - If there is a minor error in scanning then `ScanningRoundFailed` will be returned and another connection/sync
//       attempt will be made
//     - If a unrecoverable error occurs the `RecoveryFailed` event will be returned and the client will need to start
//       a new process.
sealed class WalletRestorationResult {
    class ConnectingToBaseNode : WalletRestorationResult()
    class ConnectedToBaseNode : WalletRestorationResult()
    class ConnectionToBaseNodeFailed(val retryCount: Long, val retryLimit: Long) : WalletRestorationResult() {
        override fun toString(): String = "${this.javaClass.simpleName} $retryCount / $retryLimit"
    }
    class Progress(val currentBlock: Long, val numberOfBlocks: Long) : WalletRestorationResult()
    class Completed(val numberOfUTXO: Long, val microTari: ByteArray) : WalletRestorationResult()
    class ScanningRoundFailed(val retryCount: Long, val retryLimit: Long) : WalletRestorationResult() {
        override fun toString(): String = "${this.javaClass.simpleName} $retryCount / $retryLimit"
    }
    class RecoveryFailed : WalletRestorationResult()

    companion object {
        fun create(event: Int, firstArg: ByteArray, secondArgs: ByteArray) : WalletRestorationResult {
            val first = bytesToLong(firstArg)
            val second = bytesToLong(secondArgs)
            Logger.t("WalletRestorationResult $event $first $second")
            return when(event) {
                0 -> ConnectingToBaseNode()
                1 -> ConnectedToBaseNode()
                2 -> ConnectionToBaseNodeFailed(first, second)
                3 -> Progress(first, second)
                4 -> Completed(first, secondArgs)
                5 -> ScanningRoundFailed(first, second)
                6 -> RecoveryFailed()
                else -> TODO()
            }
        }

        private fun bytesToLong(bytes: ByteArray): Long = ByteBuffer.allocate(java.lang.Long.BYTES).apply {
            put(bytes)
            flip()
        }.long
    }
}