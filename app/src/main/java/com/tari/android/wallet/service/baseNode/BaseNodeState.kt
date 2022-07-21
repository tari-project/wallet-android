package com.tari.android.wallet.service.baseNode

sealed class BaseNodeState {
    object Syncing : BaseNodeState()

    object Online : BaseNodeState()

    object Offline : BaseNodeState()
}