package com.tari.android.wallet.service.baseNode

sealed class BaseNodeState {
    object SyncStarted : BaseNodeState()
    data class SyncCompleted(val isSuccess: Boolean) : BaseNodeState()
}