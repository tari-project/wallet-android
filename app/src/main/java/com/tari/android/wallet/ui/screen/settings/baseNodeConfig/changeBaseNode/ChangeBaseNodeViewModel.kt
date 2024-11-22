package com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.CustomBaseNodeState
import com.tari.android.wallet.ui.screen.send.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.adapter.BaseNodeViewHolderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ChangeBaseNodeViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    @Inject
    lateinit var baseNodeStateHandler: BaseNodeStateHandler

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    private val _baseNodeList = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val baseNodeList: LiveData<MutableList<CommonViewHolderItem>> = _baseNodeList

    private val _customBaseNodeState = MutableStateFlow(CustomBaseNodeState())
    val customBaseNodeState = _customBaseNodeState.asStateFlow()

    init {
        component.inject(this)

        collectFlow(baseNodeStateHandler.baseNodeState) { loadList() }

        loadList()
    }

    fun selectBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodesManager.setBaseNode(baseNodeDto)
        walletManager.syncBaseNode()
        backPressed.postValue(Unit)
    }

    fun showQrCode(baseNodeDto: BaseNodeDto) {
        val data = deeplinkManager.getDeeplinkString(baseNodeDto.toDeeplink())
        showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = true), listOf(
                    HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                    ShareQrCodeModule(data),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
    }

    fun refresh() = loadList()

    private fun deleteBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodesManager.deleteUserBaseNode(baseNodeDto)
        loadList()
    }

    private fun loadList() {
        val currentBaseNode = baseNodesManager.currentBaseNode
        val items = mutableListOf<CommonViewHolderItem>()
        items.addAll(baseNodesManager.userBaseNodes.map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        items.addAll(baseNodesManager.baseNodeList.map { BaseNodeViewHolderItem(it, currentBaseNode, this::deleteBaseNode) })
        _baseNodeList.postValue(items)
    }

    fun chooseCustomBaseNodeClick() {
        showEditBaseNodeDialog()
    }

    private fun showEditBaseNodeDialog() {
        var saveAction: () -> Boolean = { false }
        val title = HeadModule(
            title = resourceManager.getString(R.string.add_base_node_form_title),
            rightButtonTitle = resourceManager.getString(R.string.add_base_node_action_button),
            rightButtonAction = { saveAction() },
        )
        val nameInput = InputModule(
            value = customBaseNodeState.value.customBaseNode?.name.orEmpty(),
            hint = resourceManager.getString(R.string.add_base_node_name_hint),
            isFirst = true,
            onDoneAction = { saveAction() },
        )
        val hexInput = InputModule(
            value = customBaseNodeState.value.customBaseNode?.publicKeyHex.orEmpty(),
            hint = resourceManager.getString(R.string.add_base_node_hex_hint),
            onDoneAction = { saveAction() },
        )
        val addressInput = InputModule(
            value = customBaseNodeState.value.customBaseNode?.address.orEmpty(),
            hint = resourceManager.getString(R.string.add_base_node_address_hint),
            isEnd = true,
            onDoneAction = { saveAction() },
        )
        saveAction = {
            customBaseNodeEntered(nameInput.value, hexInput.value, addressInput.value)
            true
        }

        showInputModalDialog(
            title,
            nameInput,
            hexInput,
            addressInput,
        )
    }

    private fun customBaseNodeEntered(enteredName: String, enteredHex: String, enteredAddress: String) {
        if (baseNodesManager.isValidBaseNode("$enteredHex::$enteredAddress")) {
            val baseNode = BaseNodeDto(
                name = enteredName.takeIf { it.isNotBlank() } ?: resourceManager.getString(R.string.add_base_node_default_name_custom),
                publicKeyHex = enteredHex,
                address = enteredAddress,
                isCustom = true,
            )
            _customBaseNodeState.update { it.copy(customBaseNode = baseNode) }

            baseNodesManager.addUserBaseNode(baseNode)
            baseNodesManager.setBaseNode(baseNode)
            walletManager.syncBaseNode()
            hideDialog()
            loadList()
        } else {
            _customBaseNodeState.update { it.copy(customBaseNode = null) }
            showModularDialog(
                SimpleDialogArgs(
                    title = resourceManager.getString(R.string.common_error_title),
                    description = resourceManager.getString(R.string.restore_from_seed_words_form_error_message),
                ).getModular(resourceManager)
            )
        }
    }
}