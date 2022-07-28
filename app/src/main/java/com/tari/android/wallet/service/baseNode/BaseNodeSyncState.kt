package com.tari.android.wallet.service.baseNode

sealed class BaseNodeSyncState {
    object NotStarted : BaseNodeSyncState()

    object Syncing : BaseNodeSyncState()

    object Online : BaseNodeSyncState()

    object Failed : BaseNodeSyncState()
}