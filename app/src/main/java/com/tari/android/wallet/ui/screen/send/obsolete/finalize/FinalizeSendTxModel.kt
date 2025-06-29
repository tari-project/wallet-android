package com.tari.android.wallet.ui.screen.send.obsolete.finalize

import com.tari.android.wallet.model.TxId

class FinalizeSendTxModel {
    data class UiState(
        val steps: List<FinalizeSendTxViewModel.FinalizingStep>,
        val sentTxId: TxId? = null,
        val txFailureReason: TxFailureReason? = null,
    ) {
        val isSuccess: Boolean
            get() = steps.all { it.isStarted && it.isCompleted } && txFailureReason == null
    }

    sealed class Effect {
        data object SendTxSuccess : Effect()
        data class ShowError(val reason: TxFailureReason) : Effect()
        data class ShowNextStep(val step: FinalizeSendTxViewModel.FinalizingStep) : Effect()
    }

    enum class TxFailureReason {
        NETWORK_CONNECTION_ERROR,
        BASE_NODE_CONNECTION_ERROR,
        SEND_ERROR;
    }
}