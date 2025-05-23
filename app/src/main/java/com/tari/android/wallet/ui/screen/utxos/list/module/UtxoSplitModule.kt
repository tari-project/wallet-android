package com.tari.android.wallet.ui.screen.utxos.list.module

import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class UtxoSplitModule(val items: List<TariUtxo>, val previewMaker: (count: Int, items: List<TariUtxo>) -> TariCoinPreview) : IDialogModule() {
    var count = 0
}