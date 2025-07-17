package com.tari.android.wallet.ui.screen.send.obsolete.finalize

import android.os.Bundle
import androidx.activity.viewModels
import com.orhanobut.logger.Logger
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.parcelable
import yat.android.ui.transactions.outcoming.TransactionState
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionActivity

class YatFinalizeSendTxActivity : YatLibOutcomingTransactionActivity() {

    val viewModel: FinalizeSendTxViewModel by viewModels()

    private val logger
        get() = Logger.t(YatFinalizeSendTxActivity::class.simpleName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.parcelable<TransactionData>(FinalizeSendTxViewModel.KEY_TRANSACTION_DATA)?.let {
            subscribeOnUI()
        } ?: run {
            logger.e("Transaction data is null. Finishing activity.")
            finish()
            return
        }
    }

    private fun subscribeOnUI() = with(viewModel) {
        collectFlow(effect) {
            when (it) {
                is FinalizeSendTxModel.Effect.SendTxSuccess -> setTransactionState(TransactionState.Complete)
                is FinalizeSendTxModel.Effect.ShowError -> setTransactionState(TransactionState.Failed)
                else -> Unit
            }
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.onYatSendTxStop()
    }
}