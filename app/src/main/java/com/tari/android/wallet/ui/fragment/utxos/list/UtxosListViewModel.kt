package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.TariOutputs
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.maxTileHeight
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.minTileHeight
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.JoinSplitButtonsState
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.Ordering
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ScreenState
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.module.ListItemModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UtxosListViewModel : CommonViewModel() {

    val screenState: MutableLiveData<ScreenState> = MutableLiveData(ScreenState.Loading)
    val joinSplitButtonsState: MutableLiveData<JoinSplitButtonsState> = MutableLiveData(JoinSplitButtonsState.None)

    val listType: MutableLiveData<ListType> = MutableLiveData()

    val ordering = MutableLiveData(Ordering.ValueDesc)

    val sortingMediator = MediatorLiveData<Unit>()

    val selectionState = MutableLiveData<Boolean>()

    val sourceList: MutableLiveData<Map<Ordering, MutableList<UtxosViewHolderItem>>> = MutableLiveData()
    val textList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val leftTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())
    val rightTileList: MutableLiveData<MutableList<UtxosViewHolderItem>> = MutableLiveData(mutableListOf())

    var serviceConnection = TariWalletServiceConnection()
    val walletService: TariWalletService
        get() = serviceConnection.currentState.service!!

    init {
        sortingMediator.addSource(sourceList) { generateFromScratch() }
        sortingMediator.addSource(ordering) { generateFromScratch() }
        setSelectionState(false)

        serviceConnection.connection.subscribe {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) loadUtxosFromFFI()
        }.addTo(compositeDisposable)

        component.inject(this)
    }

    fun selectItem(item: UtxosViewHolderItem) {
        if (item.selectionState.value) {
            item.checked.value = !item.checked.value
            recountCheckedStates()
        } else {
            showDetailedDialog()
        }
    }

    fun setSelectionStateTrue(): Boolean {
        if (!selectionState.value!!) {
            setSelectionState(true)
        }
        return true
    }

    fun setTypeList(listType: ListType) = this.listType.postValue(listType)

    fun setSelectionState(isSelecting: Boolean) {
        selectionState.postValue(isSelecting)
        textList.value?.forEach { it.selectionState.value = isSelecting }
        if (!isSelecting) {
            textList.value?.forEach { it.checked.value = false }
        }
        recountCheckedStates()
    }

    fun showOrderingSelectionDialog() {
        setSelectionState(false)
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

    fun join() {
        showConfirmDialog(R.string.utxos_join_description) {
            walletService.getWithError { error, wallet ->
                wallet.joinUtxos(error)
                _dissmissDialog.postValue(Unit)
                //todo success?
                showSuccessDialog()
            }
            //todo do join
        }
    }

    fun split() {
        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.utxos_combine_and_break_title)),
                BodyModule(resourceManager.getString(R.string.utxos_combine_and_break_description)),
                //todo one more module
                ButtonModule(resourceManager.getString(R.string.utxos_combine_and_break_button), ButtonStyle.Normal) {
                    _dissmissDialog.postValue(Unit)
                    showConfirmDialog(R.string.utxos_break_description) {
                        walletService.getWithError { error, wallet ->
                            wallet.splitUtxos(error)
                            _dissmissDialog.postValue(Unit)
                            //todo success?
                            showSuccessDialog()
                        }
                        //todo do split
                    }
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(modularDialogArgs)
    }

    private fun loadUtxosFromFFI() = viewModelScope.launch(Dispatchers.IO) {
        val map = mapOf(
            Pair(Ordering.ValueDesc, loadUtxosFromFFI(Ordering.ValueDesc)),
            Pair(Ordering.ValueAnc, loadUtxosFromFFI(Ordering.ValueAnc)),
            Pair(Ordering.DateDesc, loadUtxosFromFFI(Ordering.DateDesc)),
            Pair(Ordering.DateAnc, loadUtxosFromFFI(Ordering.DateAnc)),
        )
        val state = if (map[Ordering.ValueAnc]!!.isEmpty()) ScreenState.Empty else ScreenState.Data
        screenState.postValue(state)
        sourceList.postValue(map)
    }

    private fun loadUtxosFromFFI(sorting: Ordering): MutableList<UtxosViewHolderItem> {
        val items = mutableListOf<TariUtxo>()
        var newItems: TariOutputs
        var pageNumber = 0
        do {
            newItems = walletService.getWithError { error, wallet -> wallet.getUtxos(pageNumber, pageSize, sorting.value, error) }
            items.addAll(newItems.itemsList)
            pageNumber++
        } while (newItems.len == pageSize.toLong())
        return items.map { UtxosViewHolderItem.fromUtxo(it, resourceManager) }.toMutableList()
    }

    private fun generateFromScratch() {
        val sourceList = this.sourceList.value ?: return
        val ordering = this.ordering.value ?: return

        val orderedList = sourceList[ordering]!!.toMutableList()

        calculateHeight(orderedList)
        textList.postValue(orderedList)
        orderTileLists(orderedList)
    }

    private fun calculateHeight(list: MutableList<UtxosViewHolderItem>) {
        if (list.isEmpty()) return
        val min = list.minOf { it.source.value.tariValue }
        val max = list.maxOf { it.source.value.tariValue }

        var amountDiff = (max - min).toDouble()
        if (amountDiff == 0.0) {
            amountDiff = min.toDouble()
        }
        val heightDiff = maxTileHeight - minTileHeight
        val scale = heightDiff / amountDiff

        list.forEach {
            val calculatedHeight = ((it.source.value.tariValue - min).toDouble() * scale + minTileHeight).toInt()
            it.heigth = calculatedHeight
        }
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

    private fun recountCheckedStates() {
        if (selectionState.value == true) {
            val state = when (textList.value.orEmpty().count { it.checked.value }) {
                0 -> JoinSplitButtonsState.None
                1 -> JoinSplitButtonsState.Break
                else -> JoinSplitButtonsState.JoinAndBreak
            }
            joinSplitButtonsState.postValue(state)
        } else {
            joinSplitButtonsState.postValue(JoinSplitButtonsState.None)
        }
    }

    private fun showConfirmDialog(message: Int, action: () -> Unit) {
        val modularArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.common_are_you_sure)),
                BodyModule(resourceManager.getString(message)),
                ButtonModule(resourceManager.getString(R.string.common_lets_do_it), ButtonStyle.Normal, action),
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(modularArgs)
    }

    private fun showDetailedDialog() {
        val modularArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                ButtonModule(resourceManager.getString(R.string.utxos_break_title), ButtonStyle.Normal) {
                    _dissmissDialog.postValue(Unit)
                    split()
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(modularArgs)
    }

    private fun showSuccessDialog() {
        val modularArgs = ModularDialogArgs(
            DialogArgs() {
                setSelectionState(false)
            }, listOf(
                ImageModule(R.drawable.ic_utxos_succes_popper),
                HeadModule(resourceManager.getString(R.string.utxos_success_title)),
                BodyModule(resourceManager.getString(R.string.utxos_success_description)),
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(modularArgs)
    }

    companion object {
        const val pageSize = 1
    }
}