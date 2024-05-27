package com.tari.android.wallet.ui.dialog.modular

data class DialogArgs(
    val cancelable: Boolean = true,
    val canceledOnTouchOutside: Boolean = true,
    val isRefreshing: Boolean = false,
    val onDismiss: () -> Unit = {}
)