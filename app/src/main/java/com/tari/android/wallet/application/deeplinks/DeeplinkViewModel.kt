package com.tari.android.wallet.application.deeplinks

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import javax.inject.Inject

class DeeplinkViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var baseNodeRepository: BaseNodeSharedRepository

    init {
        component.inject(this)
    }

    fun executeAction(context: Context, deeplink: DeepLink.AddBaseNode) {
        val baseNode = BaseNodeDto.fromDeeplink(deeplink)
        val args = ConfirmDialogArgs(
            resourceManager.getString(R.string.home_custom_base_node_title),
            resourceManager.getString(R.string.home_custom_base_node_description),
            resourceManager.getString(R.string.home_custom_base_node_no_button),
            resourceManager.getString(R.string.common_lets_do_it),
            onConfirm = { addBaseNode(baseNode) }
        ).getModular(baseNode, resourceManager)
        ModularDialog(context, args).show()
    }

    private fun addBaseNode(baseNodeDto: BaseNodeDto) {
        baseNodeRepository.addUserBaseNode(baseNodeDto)
        baseNodes.setBaseNode(baseNodeDto)
    }
}