package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import javax.inject.Inject

class BaseNodeConfigViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodeSharedRepository: BaseNodeSharedRepository

    @Inject
    lateinit var baseNodes: BaseNodes

    private val _navigation = SingleLiveEvent<BaseNodeConfigNavigation>()
    val navigation: LiveData<BaseNodeConfigNavigation> = _navigation

    private val _currentBaseNode = MutableLiveData<BaseNodeDto>()
    val currentBaseNode: LiveData<BaseNodeDto> = _currentBaseNode

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    init {
        component.inject(this)

        EventBus.baseNodeSyncState.subscribe(this) { updateCurrentBaseNode(it) }
    }

    fun navigateToAdd() = _navigation.postValue(BaseNodeConfigNavigation.ToAddCustomBaseNode)

    fun navigateToChange() = _navigation.postValue(BaseNodeConfigNavigation.ToChangeBaseNode)

    private fun updateCurrentBaseNode(baseNodeSyncState: BaseNodeSyncState) {
        val syncStatus = when (baseNodeSyncState) {
            BaseNodeSyncState.Syncing -> resourceManager.getString(R.string.debug_base_node_syncing)
            BaseNodeSyncState.Online -> resourceManager.getString(R.string.debug_base_node_sync_successful)
            else -> resourceManager.getString(R.string.debug_base_node_sync_failed)
        }
        _syncStatus.postValue(syncStatus)
        _currentBaseNode.postValue(baseNodeSharedRepository.currentBaseNode)
    }
}