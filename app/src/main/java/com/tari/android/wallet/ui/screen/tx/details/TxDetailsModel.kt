package com.tari.android.wallet.ui.screen.tx.details

import androidx.annotation.StringRes
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.util.extension.safeCastTo

object TxDetailsModel {
    const val TX_EXTRA_KEY = "TX_EXTRA_KEY"

    const val BLOCK_EXPLORER_FORMAT = "%s/kernel_search?nonces=%s&amp;signatures=%s"

    data class UiState(
        val tx: Tx,
        private val blockExplorerUrl: String?,
        val requiredConfirmationCount: Long,
        val contact: ContactDto? = null,
        val explorerLink: String? = null,

        val screenTitle: String = "Transaction Details", // TODO
    ) {
        val cancellationReason: Int?
            @StringRes get() = when (tx.safeCastTo<CancelledTx>()?.cancellationReason) {
                FFITxCancellationReason.Unknown -> R.string.tx_details_cancellation_reason_unknown
                FFITxCancellationReason.UserCancelled -> R.string.tx_details_cancellation_reason_user_cancelled
                FFITxCancellationReason.Timeout -> R.string.tx_details_cancellation_reason_timeout
                FFITxCancellationReason.DoubleSpend -> R.string.tx_details_cancellation_reason_double_spend
                FFITxCancellationReason.Orphan -> R.string.tx_details_cancellation_reason_orphan
                FFITxCancellationReason.TimeLocked -> R.string.tx_details_cancellation_reason_time_locked
                FFITxCancellationReason.InvalidTransaction -> R.string.tx_details_cancellation_reason_invalid_transaction
                FFITxCancellationReason.AbandonedCoinbase -> R.string.tx_details_cancellation_reason_abandoned_coinbase
                else -> null
            }

        val blockExplorerLink: String?
            get() = tx.safeCastTo<CompletedTx>()?.txKernel?.let { txKernel ->
                String.format(
                    BLOCK_EXPLORER_FORMAT,
                    explorerLink.orEmpty(),
                    txKernel.publicNonce,
                    txKernel.signature,
                )
            }
    }
}