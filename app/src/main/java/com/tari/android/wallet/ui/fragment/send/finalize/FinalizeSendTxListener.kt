package com.tari.android.wallet.ui.fragment.send.finalize

import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.ui.fragment.send.common.TransactionData

/**
 * Listener interface - to be implemented by the host activity.
 */
interface FinalizeSendTxListener {

    fun onSendTxFailure(transactionData: TransactionData, txFailureReason: TxFailureReason)

    fun onSendTxSuccessful(txId: TxId, transactionData: TransactionData)

}