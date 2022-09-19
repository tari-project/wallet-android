package com.tari.android.wallet.service.baseNode

sealed class BaseNodeState {
    object Syncing : BaseNodeState()

    object Online : BaseNodeState()

    object Offline : BaseNodeState()

    fun toInt(): Int = when (this) {
        Syncing -> 0
        Offline -> 1
        Online -> 2
    }

    companion object {
        fun parseInt(state: Int): BaseNodeState = when (state) {
            0 -> Syncing
            1 -> Offline
            2 -> Online
            else -> Syncing
        }
    }
}