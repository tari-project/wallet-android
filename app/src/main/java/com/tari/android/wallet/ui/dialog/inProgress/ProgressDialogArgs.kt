package com.tari.android.wallet.ui.dialog.inProgress

class ProgressDialogArgs(
    val title: String,
    val description: String,
    val isShow: Boolean = true,
    val cancelable: Boolean = false,
    val canceledOnTouchOutside: Boolean = false,
    val onClose: () -> Unit = {},
)