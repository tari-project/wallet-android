package com.tari.android.wallet.ui.dialog.modular.modules.head

import androidx.annotation.DrawableRes
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class HeadModule(
    val title: String,
    val subtitle: String = "",
    val rightButtonTitle: String = "",
    @DrawableRes val rightButtonIcon: Int? = null,
    val rightButtonAction: () -> Unit = {},
) : IDialogModule()