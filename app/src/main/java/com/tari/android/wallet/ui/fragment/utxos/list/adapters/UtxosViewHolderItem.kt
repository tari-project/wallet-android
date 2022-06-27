package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.ChangedPropertyDelegate
import org.joda.time.DateTime

class UtxosViewHolderItem(
    val amount: String,
    val microTariAmount: MicroTari,
    val hash: String,
    val dateTime: DateTime,
    val formattedDateTime: String,
    val additionalTextData: String,
    val status: UtxosStatus,
    var heigth: Int = 0,
) : CommonViewHolderItem() {

    var selectionState = ChangedPropertyDelegate(false)

    var checked = ChangedPropertyDelegate(false)

    companion object {
        const val minTileHeight = 100.0
        const val maxTileHeight = 300.0
    }
}