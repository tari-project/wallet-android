package com.tari.android.wallet.ui.dialog.modular.modules.head

import androidx.annotation.DrawableRes
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class HeadModule(
    val title: String,
    val rightButtonTitle: String = "",
    @param:DrawableRes val rightButtonIcon: Int? = null,
    val rightButtonAction: () -> Unit = {},
) : IDialogModule() {
    val showRightAction: Boolean
        get() = rightButtonTitle.isNotEmpty() || rightButtonIcon != null
}