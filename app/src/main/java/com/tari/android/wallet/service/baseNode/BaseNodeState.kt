package com.tari.android.wallet.service.baseNode


enum class BaseNodeState {
    Syncing,
    Online,
    Offline;

    companion object {
        fun get(ordinal: Int): BaseNodeState{
            return enumValues<BaseNodeState>()[ordinal]
        }
    }
}