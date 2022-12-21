package com.tari.android.wallet.ui.fragment.send.finalize

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        launchObserver()
    }

    private fun subscribeOnUI() = with(viewModel) {
        observe(txFailureReason) { setTransactionState(TransactionState.Failed) }

        observe(isSuccess) { setTransactionState(TransactionState.Complete) }

        observeOnLoad(sentTxId)
        observeOnLoad(steps)
        observeOnLoad(nextStep)
    }

    private fun launchObserver() {
        viewModel.start()

        lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            viewModel.checkStepStatus()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.sentTxId.value?.let { HomeActivity.instance.get()?.onSendTxSuccessful(it, viewModel.transactionData) }

        viewModel.txFailureReason.value?.let { HomeActivity.instance.get()?.onSendTxFailure(viewModel.transactionData, it) }
    }
}