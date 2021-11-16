package com.tari.android.wallet.ui.fragment.send.finalize

import android.os.Bundle
import androidx.activity.viewModels
import com.google.gson.Gson
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.fragment.send.activity.SendTariActivity
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import yat.android.ui.transactions.outcoming.TransactionState
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity

class YatFinalizeSendTxActivity : YatLibOutcomingTransactionActivity() {

    val viewModel: FinalizeSendTxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gson = intent.getStringExtra(FinalizeSendTxViewModel.transactionDataKey)!!
        val entity =  Gson().fromJson(gson, TransactionData::class.java)

        viewModel.transactionData = entity
        subscribeOnUI()
    }

    private fun subscribeOnUI() = with(viewModel) {
        observe(txFailureReason) { setTransactionState(TransactionState.Failed) }

        observe(torConnected) { viewModel.sendTari() }

        observe(finishedSending) { setTransactionState(TransactionState.Complete) }

        observeOnLoad(sentTxId)
        observeOnLoad(currentStep)
    }

    override fun onStop() {
        super.onStop()
        viewModel.sentTxId.value?.let {
            SendTariActivity.instance.get()?.onSendTxSuccessful(it, viewModel.transactionData)
        }

        viewModel.txFailureReason.value?.let {
            SendTariActivity.instance.get()?.onSendTxFailure(viewModel.transactionData, it)
        }
    }
}