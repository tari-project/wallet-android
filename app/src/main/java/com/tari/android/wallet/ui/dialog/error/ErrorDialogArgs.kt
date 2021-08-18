package com.tari.android.wallet.ui.dialog.error

class ErrorDialogArgs(
    val title: CharSequence,
    val description: CharSequence,
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    val onClose: () -> Unit = {},
)