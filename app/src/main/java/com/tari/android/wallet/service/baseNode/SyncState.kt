package com.tari.android.wallet.service.baseNode

sealed class SyncState {
    object NotStarted : SyncState()

    object Syncing : SyncState()

    object Online : SyncState()

    object Failed : SyncState()
}