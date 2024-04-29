package com.tari.android.wallet.ui.dialog.modular

data class ModularDialogArgs(
    val dialogArgs: DialogArgs = DialogArgs(),
    val modules: List<IDialogModule> = emptyList(),
)