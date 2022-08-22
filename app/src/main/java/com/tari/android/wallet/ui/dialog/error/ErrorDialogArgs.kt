package com.tari.android.wallet.ui.dialog.error

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule

class ErrorDialogArgs(
    val title: CharSequence,
    val description: CharSequence,
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    val onClose: () -> Unit = {},
) {
    fun getModular(resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(cancelable, canceledOnTouchOutside, false, onClose), modules = listOf(
            HeadModule(title.toString()),
            BodyModule(description.toString()),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close, onClose)
        )
    )
}

