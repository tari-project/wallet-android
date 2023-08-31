package com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.changeBaseNode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.changeBaseNode.adapter.BaseNodeViewHolderItem
import javax.inject.Inject

class ChangeBaseNodeViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodeSharedRepository: BaseNodeSharedRepository

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    private val _baseNodeList = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val baseNodeList: LiveData<MutableList<CommonViewHolderItem>> = _baseNodeList

    init {
        component.inject(this)

        EventBus.baseNodeState.subscribe(this) { loadList() }

        loadList()
    }

    fun selectBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodes.setBaseNode(baseNodeDto)
        _backPressed.postValue(Unit)
    }

    fun showQrCode(baseNodeDto: BaseNodeDto) {
        val data = deeplinkHandler.getDeeplink(BaseNodeDto.getDeeplink(baseNodeDto))
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = true), listOf(
                HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                ShareQrCodeModule(data),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    fun refresh() = loadList()

    private fun deleteBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodeSharedRepository.deleteUserBaseNode(baseNodeDto)
        loadList()
    }

    private fun loadList() {
        val currentBaseNode = baseNodeSharedRepository.currentBaseNode
        val items = mutableListOf<CommonViewHolderItem>()
        items.addAll(baseNodeSharedRepository.userBaseNodes.orEmpty().map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        items.addAll(baseNodes.baseNodeList.map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        _baseNodeList.postValue(items)
    }
}