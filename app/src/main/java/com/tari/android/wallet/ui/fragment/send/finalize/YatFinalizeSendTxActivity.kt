package com.tari.android.wallet.ui.fragment.send.finalize

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.extension.parcelable
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

        intent.parcelable<TransactionData>(FinalizeSendTxViewModel.KEY_TRANSACTION_DATA)?.let { transactionData ->
            viewModel.transactionData = transactionData

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
            while (viewModel.txFailureReason.value == null && viewModel.isSuccess.value == null) {
                delay(200)
                viewModel.checkStepStatus()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.onYatSendTxStop()
    }
}