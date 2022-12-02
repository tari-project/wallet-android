package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.ChangedPropertyDelegate
import com.tari.android.wallet.util.WalletUtil
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.*

class UtxosViewHolderItem(val source: TariUtxo, var height: Int = 0) : CommonViewHolderItem() {

    var selectionState = ChangedPropertyDelegate(false)
    var checked = ChangedPropertyDelegate(false)

    val isShowDate: Boolean = source.timestamp != 0L
    val isShowMinedHeight: Boolean = source.minedHeight != 0L
    val amount = WalletUtil.amountFormatter.format(source.value.tariValue)!!
    val formattedDate: String
    val formattedTime: String
    val status: UtxosStatus?
    val isSelectable: Boolean
    val isShowingStatus: Boolean

    init {
        val dateTime = DateTime.now().withMillis(source.timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        formattedDate = format.format(dateTime.toDate()).split(" ")[0]
        formattedTime = dateTime.toString()

        status = when (source.status) {
            TariUtxo.UtxoStatus.Unspent -> UtxosStatus.Mined
            TariUtxo.UtxoStatus.EncumberedToBeReceived,
            TariUtxo.UtxoStatus.UnspentMinedUnconfirmed -> UtxosStatus.Confirmed
            else -> null
        }
        isSelectable = status == UtxosStatus.Mined
        isShowingStatus = status != null
    }

    companion object {
        const val minTileHeight = 100.0
        const val maxTileHeight = 300.0
    }
}