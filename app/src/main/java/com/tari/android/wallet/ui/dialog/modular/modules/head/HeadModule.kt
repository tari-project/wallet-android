package com.tari.android.wallet.ui.dialog.modular.modules.head

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class HeadModule(val title: String, val subtitle: String = "", val rightButtonTitle: String = "", val rightButtonAction: () -> Unit = {}) :
    IDialogModule()