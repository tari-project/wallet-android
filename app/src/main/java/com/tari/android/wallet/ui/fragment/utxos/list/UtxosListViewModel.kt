package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderDirection
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderType
import org.joda.time.DateTime
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.random.Random

class UtxosListViewModel : CommonViewModel() {

    val listType: MutableLiveData<ListType> = MutableLiveData()
    val textList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())

    init {
        component.inject(this)
        setMocks(100)
    }

    fun setTypeList(listType: ListType) {
        this.listType.postValue(listType)
    }

    fun setOrderingDirection(ordering: OrderDirection) {

    }

    fun setOrderingType(type: OrderType) {

    }

    fun setSelectionState(isSelecting: Boolean) {

    }

    fun setMocks(count: Int) {
        val list = (0..count).map {
            val amount = Random.nextDouble(1.0, 100_000.0)
            val bigInteger = BigInteger.valueOf((amount * 100_000L).toLong())
            val doubleValue = MicroTari(bigInteger).tariValue.setScale(2, RoundingMode.HALF_UP)
            val newDecimal = DecimalFormat("##.00")
            newDecimal.decimalFormatSymbols.groupingSeparator = ','
            newDecimal.isGroupingUsed = true
            newDecimal.groupingSize = 3
            val value = newDecimal.format(doubleValue)
            val startDate = DateTime.now().minusWeeks(1).toDateTime().millis
            val endDate = DateTime.now().plusWeeks(1).toDateTime().millis
            val date = Random.nextLong(startDate, endDate)
            val dateTime = DateTime.now().withMillis(date)
            val guid = UUID.randomUUID().toString().lowercase()
            UtxosViewHolderItem(value, guid + guid, false, "Confirmed", dateTime)
        }
        textList.postValue(list.toMutableList())
    }
}