package com.tari.android.wallet.ui.dialog.modular.modules.input

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class InputModule(var value: String, val hint: String, val focused: Boolean = false, val onDoneAction: (name: String) -> Boolean): IDialogModule()