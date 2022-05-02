package com.tari.android.wallet.ui.dialog.tooltipDialog

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule

class TooltipDialogArgs(val title: String, val description: String) {
    fun getModular(resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(), modules = listOf(
            HeadModule(title),
            BodyModule(description),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
        )
    )
}