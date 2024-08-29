package com.tari.android.wallet.ui.dialog.modular.modules.shareOptions

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class ShareOptionsModule(
    val shareQr: () -> Unit,
    val shareDeeplink: () -> Unit,
    val shareBle: () -> Unit,
) : IDialogModule()