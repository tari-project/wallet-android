package com.tari.android.wallet.service.baseNode

sealed class BaseNodeState {
    object SyncStarted : BaseNodeState()

    object Online : BaseNodeState()

    object Offline : BaseNodeState()
}