package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import org.joda.time.DateTime

class UtxosViewHolderItem(
    val amount: String,
    val microTariAmount: MicroTari,
    val hash: String,
    val checked: Boolean,
    val dateTime: DateTime,
    val formattedDateTime: String,
    val additionalTextData: String,
    val status: UtxosStatus,
    var heigth: Int = 0,
) : CommonViewHolderItem() {

    companion object {
        const val minTileHeight = 100.0
        const val maxTileHeight = 300.0
    }
}