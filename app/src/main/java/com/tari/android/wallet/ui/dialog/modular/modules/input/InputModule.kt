package com.tari.android.wallet.ui.dialog.modular.modules.input

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

open class InputModule(
    var value: String,
    val hint: String,
    val isFirst: Boolean = false,
    val isEnd: Boolean = false,
    val onDoneAction: () -> Boolean = { true }
) : IDialogModule()