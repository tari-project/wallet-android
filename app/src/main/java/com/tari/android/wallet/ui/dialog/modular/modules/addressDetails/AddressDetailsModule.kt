package com.tari.android.wallet.ui.dialog.modular.modules.addressDetails

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class AddressDetailsModule(
    val tariWalletAddress: TariWalletAddress,
    val copyBase58: () -> Unit,
    val copyEmojis: () -> Unit,
) : IDialogModule()