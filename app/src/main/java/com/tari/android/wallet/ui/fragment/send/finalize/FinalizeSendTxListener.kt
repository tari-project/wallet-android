package com.tari.android.wallet.ui.fragment.send.finalize

import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.ui.fragment.send.common.TransactionData

/**
 * Listener interface - to be implemented by the host activity.
 */
interface FinalizeSendTxListener {

    fun onSendTxFailure(isYat: Boolean, transactionData: TransactionData, txFailureReason: TxFailureReason)

    fun onSendTxSuccessful(isYat: Boolean, txId: TxId, transactionData: TransactionData)

}