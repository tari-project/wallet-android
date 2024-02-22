package com.tari.android.wallet.ui.fragment.send.finalize

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import yat.android.ui.transactions.outcoming.TransactionState
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity

class YatFinalizeSendTxActivity : YatLibOutcomingTransactionActivity() {

    val viewModel: FinalizeSendTxViewModel by viewModels()

    private val logger
        get() = Logger.t(YatFinalizeSendTxActivity::class.simpleName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(FinalizeSendTxViewModel.transactionDataKey)
            ?.let { Gson().fromJson(it, TransactionData::class.java) }
            ?.let { entity ->
                viewModel.transactionData = entity

                subscribeOnUI()
                launchObserver()
            } ?: run {
            logger.e("Transaction data is null. Finishing activity.")
            finish()
            return
        }
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
        viewModel.sentTxId.value?.let { viewModel.tariNavigator.onSendTxSuccessful(true, it) }

        viewModel.txFailureReason.value?.let { viewModel.tariNavigator.onSendTxFailure(true, it) }
    }
}