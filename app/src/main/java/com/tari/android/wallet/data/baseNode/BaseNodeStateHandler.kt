package com.tari.android.wallet.data.baseNode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
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

    /**
     * Update base node state.
     * @return true if the state is updated, false otherwise.
     */
    fun updateState(newState: BaseNodeState): Boolean {
        return if (newState != _baseNodeState.value) {
            _baseNodeState.update { newState }
            true
        } else false
    }

    fun updateSyncState(state: BaseNodeSyncState) {
        _baseNodeSyncState.update { state }
    }

    suspend fun doOnBaseNodeOnline(action: suspend () -> Unit) = withContext(Dispatchers.IO) {
        baseNodeState.firstOrNull { it == BaseNodeState.Online }
            ?.let { action() }
    }
}