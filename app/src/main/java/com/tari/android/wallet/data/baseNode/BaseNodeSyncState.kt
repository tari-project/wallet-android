package com.tari.android.wallet.data.baseNode

sealed class BaseNodeSyncState {
    data object NotStarted : BaseNodeSyncState()
    data object Syncing : BaseNodeSyncState()
    data object Online : BaseNodeSyncState()
    data object Failed : BaseNodeSyncState()
}