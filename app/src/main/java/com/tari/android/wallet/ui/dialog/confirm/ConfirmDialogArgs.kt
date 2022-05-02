package com.tari.android.wallet.ui.dialog.confirm

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody.CustomBaseNodeBodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule

class ConfirmDialogArgs(
    val title: CharSequence,
    val description: CharSequence,
    val cancelButtonText: CharSequence? = null,
    val confirmButtonText: CharSequence? = null,
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismiss: () -> Unit = {}
) {
    fun getModular(resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(cancelable, canceledOnTouchOutside, onDismiss), modules = listOf(
            HeadModule(title.toString()),
            BodyModule(description.toString()),
            ButtonModule(confirmButtonText?.toString() ?: resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal, onConfirm),
            ButtonModule(cancelButtonText?.toString() ?: resourceManager.getString(R.string.common_cancel), ButtonStyle.Close, onCancel)
        )
    )

    fun getModular(baseNode: BaseNodeDto, resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(cancelable, canceledOnTouchOutside, onDismiss), modules = listOf(
            HeadModule(title.toString()),
            BodyModule(description.toString()),
            CustomBaseNodeBodyModule(baseNode),
            ButtonModule(confirmButtonText?.toString() ?: resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal, onConfirm),
            ButtonModule(cancelButtonText?.toString() ?: resourceManager.getString(R.string.common_cancel), ButtonStyle.Close, onCancel)
        )
    )
}