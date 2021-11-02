package com.tari.android.wallet.ui.fragment.send.finalize

import android.os.Bundle
import androidx.activity.viewModels
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.fragment.send.activity.SendTariActivity
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import yat.android.ui.transactions.outcoming.TransactionState
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity

class YatFinalizeSendTxActivity : YatLibOutcomingTransactionActivity() {

    val viewModel: FinalizeSendTxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.transactionData = intent.getSerializableExtra(FinalizeSendTxViewModel.transactionDataKey) as TransactionData
        subscribeOnUI()
    }

    private fun subscribeOnUI() = with(viewModel) {
        observe(txFailureReason) {
            setTransactionState(TransactionState.Failed)
            SendTariActivity.instance.get()?.onSendTxFailure(viewModel.transactionData, it)
        }

        observe(sentTxId) {
            setTransactionState(TransactionState.Complete)
            SendTariActivity.instance.get()?.onSendTxSuccessful(it, viewModel.transactionData)
        }

        observe(torConnected) { viewModel.sendTari() }
    }
}