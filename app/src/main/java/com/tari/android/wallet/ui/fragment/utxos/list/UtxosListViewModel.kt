package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosStatus
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.maxTileHeight
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.minTileHeight
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

    val orderingDirection = MutableLiveData<OrderDirection>()
    val orderingType = MutableLiveData<OrderType>()

    val sortingMediator = MediatorLiveData<Unit>()

    val sourceList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val textList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val leftTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val rightTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())

    init {
        sortingMediator.addSource(sourceList) { generateFromScratch() }
        sortingMediator.addSource(orderingType) { generateFromScratch() }
        sortingMediator.addSource(orderingDirection) { generateFromScratch() }

        component.inject(this)
        setMocks(1000)
    }

    private fun generateFromScratch() {
        val sourceList = this.sourceList.value ?: return
        val orderingType = this.orderingType.value ?: return
        val orderDirection = this.orderingDirection.value ?: return

        val orderedList = when(orderDirection) {
            OrderDirection.Desc -> when(orderingType) {
                OrderType.ByValue -> sourceList.sortedByDescending { it.microTariAmount.tariValue }
                OrderType.ByDate -> sourceList.sortedByDescending { it.dateTime }
            }
            OrderDirection.Anc -> when(orderingType) {
                OrderType.ByValue -> sourceList.sortedBy { it.microTariAmount.tariValue }
                OrderType.ByDate -> sourceList.sortedBy { it.dateTime }
            }
        }.toMutableList()

        textList.postValue(orderedList)
        orderTileLists(orderedList)
    }

    fun setTypeList(listType: ListType) = this.listType.postValue(listType)

    fun setOrderingDirection(ordering: OrderDirection) = this.orderingDirection.postValue(ordering)

    fun setOrderingType(type: OrderType) = this.orderingType.postValue(type)

    fun setSelectionState(isSelecting: Boolean) = Unit

    fun setMocks(count: Int) {
        val list = (0..count).map {
            val amount = Random.nextDouble(1.0, 1_000_000.0)
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
            val status = UtxosStatus.values()[Random.nextInt(0, 3)]
            UtxosViewHolderItem(value, MicroTari(bigInteger), guid + guid, false, dateTime, status)
        }.toMutableList()
        calculateHeight(list)

        sourceList.postValue(list)
    }

    private fun calculateHeight(list: MutableList<UtxosViewHolderItem>) {
        val min = list.minOf { it.microTariAmount.tariValue }
        val max = list.maxOf { it.microTariAmount.tariValue }

        val amountDiff = (max - min).toDouble()
        val heightDiff = maxTileHeight - minTileHeight
        val scale = heightDiff / amountDiff

        list.forEach { it.heigth = ((it.microTariAmount.tariValue - min).toDouble() * scale + minTileHeight).toInt() }
    }

    private fun orderTileLists(list: MutableList<UtxosViewHolderItem>) {
        val leftList = mutableListOf<UtxosViewHolderItem>()
        val rightList = mutableListOf<UtxosViewHolderItem>()
        var leftHeight = 0
        var rightHeight = 0
        val marginSize = resourceManager.getDimen(R.dimen.utxos_list_tile_margin).toInt()
        for (item in list) {
            if (leftHeight <= rightHeight) {
                leftList.add(item)
                leftHeight += item.heigth + marginSize
            } else {
                rightList.add(item)
                rightHeight += item.heigth + marginSize
            }
        }
        leftTileList.postValue(leftList)
        rightTileList.postValue(rightList)
    }
}