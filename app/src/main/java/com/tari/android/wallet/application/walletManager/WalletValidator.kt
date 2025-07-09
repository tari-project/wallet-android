package com.tari.android.wallet.application.walletManager

import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletManager.WalletValidationResult
import com.tari.android.wallet.application.walletManager.WalletManager.WalletValidationType
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.data.baseNode.BaseNodeSyncState
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class WalletValidator(
    private val walletManager: WalletManager,
    private val baseNodeStateHandler: BaseNodeStateHandler,
) {

    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    /**
     * Maps the validation type to the request id and validation result. This map will be
     * initialized at the beginning of each base node validation sequence.
     * Validation results will all be null, and will be set as the result callbacks get called.
     */
    private val walletValidationStatusMap: ConcurrentMap<WalletValidationType, WalletValidationResult> = ConcurrentHashMap()

    suspend fun validateWallet() {
        walletManager.doOnWalletRunning { wallet ->
            try {
                logger.i("Wallet validation: Starting Tx and TXO validation...")
                walletValidationStatusMap.clear()
                walletValidationStatusMap[WalletValidationType.TXO] = WalletValidationResult(wallet.startTXOValidation(), null)
                walletValidationStatusMap[WalletValidationType.TX] = WalletValidationResult(wallet.startTxValidation(), null)
                logger.i(
                    "Wallet validation: Started Tx and TXO validation with " +
                            "request keys: ${Gson().toJson(walletValidationStatusMap.map { it.value.requestKey })}"
                )
            } catch (e: Throwable) {
                logger.i("Wallet validation: Error: ${e.message}")
                walletValidationStatusMap.clear()
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Failed)
            }
        }
    }

    fun checkValidationResult(type: WalletValidationType, responseId: BigInteger, isSuccess: Boolean) {
        try {
            val currentStatus = walletValidationStatusMap[type] ?: return
            if (currentStatus.requestKey != responseId) return
            walletValidationStatusMap[type] = WalletValidationResult(currentStatus.requestKey, isSuccess)
            logger.i("Wallet validation: Validation result for request $responseId: $type: ${if (isSuccess) "Success" else "Failed!"}")
            checkBaseNodeSyncCompletion()
        } catch (e: Throwable) {
            logger.i("Wallet validation: $type validation for request $responseId failed with an error: ${e.message}")
        }
    }

    private fun checkBaseNodeSyncCompletion() {
        // make a copy of the status map for concurrency protection
        val statusMapCopy = walletValidationStatusMap.toMap()

        val failed = statusMapCopy.any { it.value.isSuccess == false }
        val inProgress = statusMapCopy.any { it.value.isSuccess == null }
        val successful = statusMapCopy.all { it.value.isSuccess == true }

        when {
            failed -> {
                walletValidationStatusMap.clear()
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Failed)
            }

            inProgress -> {
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Syncing)
            }

            successful -> {
                walletValidationStatusMap.clear()
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Online)
                logger.i("Wallet validation: Validation completed successfully")
            }
        }
    }
}