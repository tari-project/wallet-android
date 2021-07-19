package com.tari.android.wallet.ui.dialog.confirm

class ConfirmDialogArgs(
    val title: CharSequence,
    val description: CharSequence,
    val cancelButtonText: CharSequence? = null,
    val confirmButtonText: CharSequence? = null,
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    val confirmStyle: ConfirmDialog.ConfirmStyle = ConfirmDialog.ConfirmStyle.Default,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)