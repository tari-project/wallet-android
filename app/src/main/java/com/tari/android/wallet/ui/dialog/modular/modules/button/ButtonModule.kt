package com.tari.android.wallet.ui.dialog.modular.modules.button

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class ButtonModule(val text: String, val style: ButtonStyle, val action: () -> Unit = {}) : IDialogModule()