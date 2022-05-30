package com.tari.android.wallet.ui.fragment.send.addAmount.feeModule

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.fragment.send.addAmount.FeeData

class FeeModule(
    val fee: MicroTari,
    val feePerGramStats: List<FeeData>,
    var selectedSpeed: NetworkSpeed,
    var feePerGram: FeeData? = null
) : IDialogModule()