package com.tari.android.wallet.ui.dialog.modular

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule

/**
 * Simple dialog arguments for a dialog with a title, a description and a close button action.
 */
data class SimpleDialogArgs(
    @param:DrawableRes val iconRes: Int? = null,
    val title: CharSequence,
    val description: CharSequence,
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    @param:StringRes val closeButtonTextRes: Int = R.string.common_close,
    val onClose: () -> Unit = {},
) {
    fun getModular(resourceManager: ResourceManager): ModularDialogArgs = ModularDialogArgs(
        DialogArgs(cancelable, canceledOnTouchOutside, onClose), modules = listOfNotNull(
            iconRes?.let { ImageModule(it) },
            HeadModule(title.toString()),
            BodyModule(description.toString()),
            ButtonModule(resourceManager.getString(closeButtonTextRes), ButtonStyle.Close, onClose)
        )
    )
}

