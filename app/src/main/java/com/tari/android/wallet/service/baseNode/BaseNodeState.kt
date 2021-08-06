package com.tari.android.wallet.service.baseNode

import com.tari.android.wallet.model.BaseNodeValidationResult

sealed class BaseNodeState {
    object SyncStarted : BaseNodeState()
    data class SyncCompleted(val result: BaseNodeValidationResult) : BaseNodeState()
}