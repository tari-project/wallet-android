package com.tari.android.wallet.ui.component.networkStateIndicator.module

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleConnectionStatusesBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class ConnectionStatusesModuleView(context: Context, buttonModule: ConnectionStatusesModule) :
    CommonView<CommonViewModel, DialogModuleConnectionStatusesBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleConnectionStatusesBinding =
        DialogModuleConnectionStatusesBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        ui.wifiStatusText.setText(buttonModule.networkText)
        ui.torText.setText(buttonModule.torText)
        ui.baseNodeStatusText.setText(buttonModule.baseNodeStateText)
        ui.syncingStateText.setText(buttonModule.baseNodeSyncText)

        ui.networkIcon.setBackgroundResource(buttonModule.networkIcon)
        ui.torIcon.setBackgroundResource(buttonModule.torIcon)
        ui.baseNodeIcon.setBackgroundResource(buttonModule.baseNodeStateIcon)
        ui.syncIcon.setBackgroundResource(buttonModule.baseNodeSyncIcon)
    }
}