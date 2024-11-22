package com.tari.android.wallet.ui.component.networkStateIndicator.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleConnectionStatusesBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.string

@SuppressLint("ViewConstructor")
class ConnectionStatusesModuleView(context: Context, module: ConnectionStatusesModule) :
    CommonView<CommonViewModel, DialogModuleConnectionStatusesBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleConnectionStatusesBinding =
        DialogModuleConnectionStatusesBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.wifiStatusText.setText(module.networkText)
        ui.torText.setText(module.torText)
        ui.baseNodeStatusText.setText(module.baseNodeStateText)
        ui.syncingStateText.setText(module.baseNodeSyncText)

        ui.networkIcon.setBackgroundResource(module.networkIcon)
        ui.torIcon.setBackgroundResource(module.torIcon)
        ui.baseNodeIcon.setBackgroundResource(module.baseNodeStateIcon)
        ui.syncIcon.setBackgroundResource(module.baseNodeSyncIcon)

        ui.textChainTipWaiting.setVisible(module.showChainTipConnecting)
        ui.textChainTipValue.setVisible(!module.showChainTipConnecting)
        ui.textChainTipValue.text = context.string(R.string.connection_status_dialog_chain_tip_value, module.walletScannedHeight, module.chainTip)
    }
}