package com.tari.android.wallet.service.baseNode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseNodeStateHandler @Inject constructor() {

    /**
     * Base node state. Showing the wallet is connected to the base node or not.
     */
    private val _baseNodeState = MutableStateFlow(BaseNodeState.Syncing)
    val baseNodeState = _baseNodeState.asStateFlow()

    /**
     * Base node sync state. Showing the wallet validation status after connecting to the base node.
     */
    private val _baseNodeSyncState = MutableStateFlow<BaseNodeSyncState>(BaseNodeSyncState.NotStarted)
    val baseNodeSyncState = _baseNodeSyncState.asStateFlow()

    fun updateState(state: BaseNodeState) {
        _baseNodeState.update { state }
    }

    fun updateSyncState(state: BaseNodeSyncState) {
        _baseNodeSyncState.update { state }
    }
}