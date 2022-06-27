package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosStatus
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.maxTileHeight
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.minTileHeight
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.Ordering
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.module.ListItemModule
import org.joda.time.DateTime
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.random.Random

class UtxosListViewModel : CommonViewModel() {

    val listType: MutableLiveData<ListType> = MutableLiveData()

    val ordering = MutableLiveData(Ordering.ValueDesc)

    val sortingMediator = MediatorLiveData<Unit>()

    val selectionState = MutableLiveData<Boolean>()

    val sourceList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val textList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val leftTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val rightTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())

    init {
        sortingMediator.addSource(sourceList) { generateFromScratch() }
        sortingMediator.addSource(ordering) { generateFromScratch() }
        setSelectionState(false)

        component.inject(this)
        setMocks(1000)
    }

    private fun generateFromScratch() {
        val sourceList = this.sourceList.value ?: return
        val ordering = this.ordering.value ?: return

        val orderedList = when (ordering) {
            Ordering.ValueDesc -> sourceList.sortedByDescending { it.microTariAmount.tariValue }
            Ordering.ValueAnc -> sourceList.sortedBy { it.microTariAmount.tariValue }
            Ordering.DateDesc -> sourceList.sortedByDescending { it.dateTime }
            Ordering.DateAnc -> sourceList.sortedBy { it.dateTime }
        }.toMutableList()

        textList.postValue(orderedList)
        orderTileLists(orderedList)
    }

    fun setTypeList(listType: ListType) = this.listType.postValue(listType)

    fun setSelectionState(isSelecting: Boolean) {
        selectionState.postValue(isSelecting)
        sourceList.value?.forEach { it.selectionState.value = isSelecting }
        if (!isSelecting) {
            sourceList.value?.forEach { it.checked.value = false }
        }
    }

    fun showOrderingSelectionDialog() {
        val listOptions = Ordering.values().map { ListItemModule(it) }
        listOptions.firstOrNull { it.ordering == ordering.value }?.isSelected = true
        listOptions.forEach {
            it.click = {
                listOptions.forEach { it.isSelected = false }
                it.isSelected = true
            }
        }
        val modules = mutableListOf<IDialogModule>()
        modules.add(HeadModule(resourceManager.getString(R.string.utxos_ordering_title)))
        modules.addAll(listOptions)
        modules.add(ButtonModule(resourceManager.getString(R.string.common_apply), ButtonStyle.Normal) {
            ordering.postValue(listOptions.firstOrNull { it.isSelected }?.ordering)
            _dissmissDialog.postValue(Unit)
        })
        modules.add(ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close))
        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(),
            modules
        )
        _modularDialog.postValue(modularDialogArgs)
    }

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
            val formattedDate = dateTime.toString("dd/MM/yyyy")
            val formattedTime = dateTime.toString("HH:mm")
            val guid = UUID.randomUUID().toString().lowercase()
            val status = UtxosStatus.values()[Random.nextInt(0, 2)]
            val additionalTextData = resourceManager.getString(status.text) + " | " + formattedDate + " | " + formattedTime
            UtxosViewHolderItem(value, MicroTari(bigInteger), guid + guid, dateTime, formattedDate, additionalTextData, status)
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