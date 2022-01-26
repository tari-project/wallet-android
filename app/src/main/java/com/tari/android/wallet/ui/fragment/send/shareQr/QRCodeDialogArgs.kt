package com.tari.android.wallet.ui.fragment.send.shareQr

import android.content.Context

class QRCodeDialogArgs (
    val deeplink: String,
    val context: Context,
    val cancelable: Boolean = false,
    val canceledOnTouchOutside: Boolean = false,
    val onClose: () -> Unit = {},
    val shareAction: () -> Unit = {}
)