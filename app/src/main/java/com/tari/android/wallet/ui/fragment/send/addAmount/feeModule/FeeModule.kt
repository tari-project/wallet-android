package com.tari.android.wallet.ui.fragment.send.addAmount.feeModule

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class FeeModule(val fee: MicroTari, val networkSpeed: NetworkSpeed): IDialogModule()