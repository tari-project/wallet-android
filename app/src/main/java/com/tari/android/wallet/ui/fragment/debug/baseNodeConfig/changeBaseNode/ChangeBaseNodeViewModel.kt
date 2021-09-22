package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.adapter.BaseNodeViewHolderItem
import javax.inject.Inject

internal class ChangeBaseNodeViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodeSharedRepository: BaseNodeSharedRepository

    @Inject
    lateinit var baseNodes: BaseNodes

    private val _baseNodeList = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val baseNodeList: LiveData<MutableList<CommonViewHolderItem>> = _baseNodeList

    init {
        component?.inject(this)

        EventBus.baseNodeState.subscribe(this) { loadList() }

        loadList()
    }

    fun selectBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodes.setBaseNode(baseNodeDto)
        _backPressed.postValue(Unit)
    }

    private fun deleteBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodeSharedRepository.deleteUserBaseNode(baseNodeDto)
        loadList()
    }

    private fun loadList() {
        val currentBaseNode = baseNodeSharedRepository.currentBaseNode ?: return
        val items = mutableListOf<CommonViewHolderItem>()
        items.addAll(baseNodeSharedRepository.userBaseNodes.orEmpty().map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        items.addAll(baseNodes.baseNodeList.map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        _baseNodeList.postValue(items)
    }
}