package com.tari.android.wallet.ui.screen.tx.details

import androidx.annotation.StringRes
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.model.tx.Tx.Direction
import com.tari.android.wallet.util.extension.safeCastTo
import com.tari.android.wallet.util.extension.txFormattedDate
import java.util.Date

object TxDetailsModel {

    const val TX_EXTRA_KEY = "TX_EXTRA_KEY"
    const val SHOW_CLOSE_BUTTON_EXTRA_KEY = "SHOW_CLOSE_BUTTON_EXTRA_KEY"

    const val BLOCK_EXPLORER_FORMAT = "%s/kernel_search?nonces=%s&signatures=%s"

    data class UiState(
        val tx: Tx,
        val showCloseButton: Boolean,
        val ticker: String,
        private val blockExplorerBaseUrl: String?,
        val requiredConfirmationCount: Long,
        val contact: ContactDto? = null,
    ) {
        val screenTitle: Int
            @StringRes get() = when {
                tx is CancelledTx -> R.string.tx_detail_payment_cancelled

                tx.status == TxStatus.ONE_SIDED_CONFIRMED || tx.status == TxStatus.MINED_CONFIRMED || tx.status == TxStatus.IMPORTED -> {
                    if (tx.isInbound) R.string.tx_detail_payment_received else R.string.tx_detail_payment_sent
                }

                else -> R.string.tx_detail_pending_payment_received
            }

        val blockExplorerLink: String?
            get() = tx.safeCastTo<CompletedTx>()?.txKernel?.let { txKernel ->
                String.format(BLOCK_EXPLORER_FORMAT, blockExplorerBaseUrl, txKernel.publicNonce, txKernel.signature)
            }

        val tariTxnId: String?
            get() = tx.safeCastTo<CompletedTx>()?.txKernel?.publicNonce

        val txFee: MicroTari?
            get() = when {
                tx is CompletedTx && tx.isOutbound -> tx.fee
                tx is CancelledTx && tx.isOutbound -> tx.fee
                tx is PendingOutboundTx -> tx.fee
                else -> null
            }

        val formattedDate: String
            get() = Date(tx.timestamp.toLong() * 1000).txFormattedDate()

        val showCancelButton: Boolean
            get() = tx is PendingOutboundTx && tx.isOutbound && tx.status == TxStatus.PENDING

        val totalAmount: MicroTari
            get() = txFee?.let { tx.amount + it } ?: tx.amount

        val txStatusText: TxStatusText
            get() = when {
                tx is CancelledTx -> TxStatusText.Cancelled(
                    textRes = when (tx.cancellationReason) {
                        FFITxCancellationReason.UserCancelled -> R.string.tx_details_cancellation_reason_user_cancelled
                        FFITxCancellationReason.Timeout -> R.string.tx_details_cancellation_reason_timeout
                        FFITxCancellationReason.DoubleSpend -> R.string.tx_details_cancellation_reason_double_spend
                        FFITxCancellationReason.Orphan -> R.string.tx_details_cancellation_reason_orphan
                        FFITxCancellationReason.TimeLocked -> R.string.tx_details_cancellation_reason_time_locked
                        FFITxCancellationReason.InvalidTransaction -> R.string.tx_details_cancellation_reason_invalid_transaction
                        FFITxCancellationReason.AbandonedCoinbase -> R.string.tx_details_cancellation_reason_abandoned_coinbase
                        FFITxCancellationReason.Unknown -> R.string.tx_details_cancellation_reason_unknown
                        FFITxCancellationReason.NotCancelled -> R.string.tx_details_cancellation_reason_not_cancelled
                    }
                )

                tx.status == TxStatus.PENDING -> when (tx.direction) {
                    Direction.INBOUND -> TxStatusText.InProgress(R.string.tx_detail_waiting_for_sender_to_complete)
                    Direction.OUTBOUND -> TxStatusText.InProgress(R.string.tx_detail_waiting_for_recipient)
                }

                tx.status in listOf(TxStatus.BROADCAST, TxStatus.COMPLETED) -> TxStatusText.InProgressStep(
                    textRes = R.string.tx_detail_completing_final_processing_with_step,
                    step = 1,
                    stepCount = requiredConfirmationCount.toInt() + 1,
                )

                tx.status == TxStatus.MINED_UNCONFIRMED -> tx.safeCastTo<CompletedTx>()?.confirmationCount?.let { confirmationCount ->
                    TxStatusText.InProgressStep(
                        textRes = R.string.tx_detail_completing_final_processing_with_step,
                        step = confirmationCount.toInt(),
                        stepCount = requiredConfirmationCount.toInt() + 1,
                    )
                } ?: TxStatusText.InProgress(R.string.tx_detail_completing_final_processing)

                else -> TxStatusText.Completed
            }

        sealed class TxStatusText {
            data class InProgress(@StringRes val textRes: Int) : TxStatusText()
            data class InProgressStep(@StringRes val textRes: Int, val step: Int, val stepCount: Int) : TxStatusText()
            data object Completed : TxStatusText()
            data class Cancelled(@StringRes val textRes: Int) : TxStatusText()
        }
    }
}