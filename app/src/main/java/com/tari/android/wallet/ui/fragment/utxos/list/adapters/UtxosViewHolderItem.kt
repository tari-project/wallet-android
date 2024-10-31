package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.ChangedPropertyDelegate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UtxosViewHolderItem(val source: TariUtxo, networkBlockHeight: Long) : CommonViewHolderItem() {

    val amount = WalletConfig.amountFormatter.format(source.value.tariValue)!!
    val formattedDate: String = SimpleDateFormat("d MMM, y", Locale.getDefault()).format(Date(source.timestamp))
    val formattedTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(source.timestamp))
    val status: UtxosStatus? = when (source.status) {
        TariUtxo.UtxoStatus.Unspent -> UtxosStatus.Mined
        TariUtxo.UtxoStatus.EncumberedToBeReceived,
        TariUtxo.UtxoStatus.UnspentMinedUnconfirmed -> UtxosStatus.Confirmed

        else -> null
    }
    val immature: Boolean = source.lockHeight > networkBlockHeight

    var height: Int = 0
    var selectionState = ChangedPropertyDelegate(false)
    var checked = ChangedPropertyDelegate(false)

    val enabled
        get() = !immature && status == UtxosStatus.Mined
    val showDate: Boolean
        get() = source.timestamp != 0L
    val showMinedHeight: Boolean
        get() = source.minedHeight != 0L
    val showLockHeight: Boolean
        get() = source.lockHeight != 0L
    val selectable: Boolean
        get() = enabled
    val showStatus: Boolean
        get() = status != null

    override val viewHolderUUID: String = "UtxosViewHolderItem" + source.value.toString()

    companion object {
        const val MIN_TILE_HEIGHT = 100.0
        const val MAX_TILE_HEIGHT = 300.0
    }
}