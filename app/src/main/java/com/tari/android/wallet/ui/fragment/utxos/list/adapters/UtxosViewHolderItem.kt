package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.ChangedPropertyDelegate
import com.tari.android.wallet.util.WalletUtil
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import kotlin.random.Random

class UtxosViewHolderItem(
    val source: TariUtxo,
    val amount: String,
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

        fun fromUtxo(utxo: TariUtxo, resourceManager: ResourceManager): UtxosViewHolderItem {
            val value = WalletUtil.amountFormatter.format(utxo.value.tariValue)
            val startDate = DateTime.now().minusWeeks(1).toDateTime().millis
            val endDate = DateTime.now().plusWeeks(1).toDateTime().millis
            val date = Random.nextLong(startDate, endDate)
            val dateTime = DateTime.now().withMillis(date)
            val format = SimpleDateFormat()
            val formattedDate = format.format(dateTime.toDate()).split(" ")[0]
            val formattedTime = dateTime.toString("HH:mm")
            val status = UtxosStatus.values()[Random.nextInt(0, 2)]
            val additionalTextData = resourceManager.getString(status.text) + " | " + formattedDate + " | " + formattedTime
            return UtxosViewHolderItem(utxo, value, utxo.commitment, dateTime, formattedDate, additionalTextData, status)
        }
    }
}