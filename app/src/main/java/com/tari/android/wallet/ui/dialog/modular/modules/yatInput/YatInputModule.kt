package com.tari.android.wallet.ui.dialog.modular.modules.yatInput

import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule

class YatInputModule(
    val search: suspend (yat: String) -> Boolean,
    value: String,
    hint: String,
    isFirst: Boolean = false,
    isEnd: Boolean = false,
    onDoneAction: () -> Boolean = { true }
) : InputModule(value, hint, isFirst, isEnd, onDoneAction)