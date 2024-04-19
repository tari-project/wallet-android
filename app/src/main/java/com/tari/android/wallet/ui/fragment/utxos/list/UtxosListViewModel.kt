package com.tari.android.wallet.ui.fragment.utxos.list

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.getWithError
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
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.MAX_TILE_HEIGHT
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem.Companion.MIN_TILE_HEIGHT
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.JoinSplitButtonsState
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.Ordering
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ScreenState
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.module.DetailItemModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.ListItemModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoAmountModule
import com.tari.android.wallet.ui.fragment.utxos.list.module.UtxoSplitModule
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import javax.inject.Inject

class UtxosListViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    val screenState: MutableLiveData<ScreenState> = MutableLiveData(ScreenState.Loading)
    val joinSplitButtonsState: MutableLiveData<JoinSplitButtonsState> = MutableLiveData(JoinSplitButtonsState.None)

    val listType: MutableLiveData<ListType> = MutableLiveData()

    val ordering = MutableLiveData(Ordering.ValueDesc)

    val sortingMediator = MediatorLiveData<Unit>()

    val selectionState = MutableLiveData<Boolean>()

    private val sourceList: MutableLiveData<List<UtxosViewHolderItem>> = MutableLiveData(emptyList())
    val textList: MutableLiveData<List<UtxosViewHolderItem>> = MutableLiveData(emptyList())
    val leftTileList: MutableLiveData<List<UtxosViewHolderItem>> = MutableLiveData(emptyList())
    val rightTileList: MutableLiveData<List<UtxosViewHolderItem>> = MutableLiveData(emptyList())

    init {
        sortingMediator.addSource(sourceList) { generateFromScratch() }
        sortingMediator.addSource(ordering) { generateFromScratch() }
        setSelectionState(false)

        doOnConnected { loadUtxosFromFFI() }

        component.inject(this)
    }

    fun selectItem(item: UtxosViewHolderItem) {
        if (item.selectionState.value) {
            item.checked.value = !item.checked.value
            recountCheckedStates()
        } else {
            if (selectionState.value != true) {
                showDetailedDialog(item)
            }
        }
    }

    fun setSelectionStateTrue(item: UtxosViewHolderItem): Boolean {
        if (!selectionState.value!!) {
            setSelectionState(true)
            selectItem(item)
        }
        return true
    }

    fun setTypeList(listType: ListType) = this.listType.postValue(listType)

    fun setSelectionState(isSelecting: Boolean) {
        selectionState.postValue(isSelecting)
        val selectable = textList.value?.filter { it.selectable }.orEmpty()
        selectable.forEach { it.selectionState.value = isSelecting }
        if (!isSelecting) {
            selectable.forEach { it.checked.value = false }
        }
        recountCheckedStates()
    }

    fun showOrderingSelectionDialog() {
        setSelectionState(false)
        val listOptions = Ordering.entries.map { ListItemModule(it) }
        listOptions.firstOrNull { it.ordering == ordering.value }?.isSelected = true
        listOptions.forEach {
            it.click = {
                listOptions.forEach { option -> option.isSelected = false }
                it.isSelected = true
            }
        }
        val modules = mutableListOf<IDialogModule>()
        modules.add(HeadModule(resourceManager.getString(R.string.utxos_ordering_title)))
        modules.addAll(listOptions)
        modules.add(ButtonModule(resourceManager.getString(R.string.common_apply), ButtonStyle.Normal) {
            ordering.postValue(listOptions.firstOrNull { it.isSelected }?.ordering)
            dismissDialog.postValue(Unit)
        })
        modules.add(ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close))
        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(),
            modules
        )
        modularDialog.postValue(modularDialogArgs)
    }

    fun join() {
        showConfirmDialog(R.string.utxos_join_description) {
            walletService.getWithError { error, wallet ->
                val selectedUtxos = textList.value.orEmpty().filter { it.checked.value }.map { it.source }.toList()
                wallet.joinUtxos(selectedUtxos, error)
                dismissDialog.postValue(Unit)
                loadUtxosFromFFI()
                EventBus.post(Event.Transaction.Updated)
                showSuccessJoinDialog()
            }
        }
    }

    fun split(currentItem: UtxosViewHolderItem? = null) {
        val selectedUtxos = textList.value.orEmpty().filter { it.checked.value }.map { it.source }.toMutableList()
        currentItem?.let { selectedUtxos.add(it.source) }
        val splitModule = UtxoSplitModule(selectedUtxos) { count, items ->
            walletService.getWithError { error, wallet -> wallet.previewSplitUtxos(items, count, error) }
        }
        val title = if (selectedUtxos.count() > 1) R.string.utxos_combine_and_break_title else R.string.utxos_break_title
        val buttonText = if (selectedUtxos.count() > 1) R.string.utxos_combine_and_break_button else R.string.utxos_break_button
        val modularDialogArgs = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(title)),
                BodyModule(resourceManager.getString(R.string.utxos_combine_and_break_description)),
                splitModule,
                ButtonModule(resourceManager.getString(buttonText), ButtonStyle.Normal) {
                    dismissDialog.postValue(Unit)
                    showConfirmDialog(R.string.utxos_break_description) {
                        walletService.getWithError { error, wallet ->
                            wallet.splitUtxos(selectedUtxos, splitModule.count, error)
                            dismissDialog.postValue(Unit)
                            loadUtxosFromFFI()
                            EventBus.post(Event.Transaction.Updated)
                            showSuccessSplitDialog()
                        }
                    }
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(modularDialogArgs)
    }

    private fun loadUtxosFromFFI() {
        val allItems = if (DebugConfig.mockUtxos) {
            MockDataStub.createUtxoList()
        } else {
            walletService.getWithError { error, wallet ->
                wallet.getAllUtxos(error)
            }.itemsList.map { UtxosViewHolderItem(it, baseNodesManager.networkBlockHeight.toLong()) }.filter { it.showStatus }
        }

        val state = if (allItems.isEmpty()) ScreenState.Empty else ScreenState.Data
        screenState.postValue(state)

        sourceList.postValue(allItems)
    }

    private fun generateFromScratch() {
        val sourceList = this.sourceList.value ?: return
        val ordering = this.ordering.value ?: return

        val orderedList = when (ordering) {
            Ordering.ValueAnc -> sourceList.sortedBy { it.source.value }
            Ordering.ValueDesc -> sourceList.sortedByDescending { it.source.value }
            Ordering.DateAnc -> sourceList.sortedBy { it.source.timestamp }
            Ordering.DateDesc -> sourceList.sortedByDescending { it.source.timestamp }
        }.toMutableList()

        calculateHeight(orderedList)
        textList.postValue(orderedList)
        orderTileLists(orderedList)
    }

    private fun calculateHeight(list: List<UtxosViewHolderItem>) {
        if (list.isEmpty()) return
        val min = list.minOf { it.source.value.tariValue }
        val max = list.maxOf { it.source.value.tariValue }

        var amountDiff = (max - min).toDouble()
        if (amountDiff == 0.0) {
            amountDiff = min.toDouble()
        }
        val heightDiff = MAX_TILE_HEIGHT - MIN_TILE_HEIGHT
        val scale = heightDiff / amountDiff

        list.forEach {
            val calculatedHeight = ((it.source.value.tariValue - min).toDouble() * scale + MIN_TILE_HEIGHT).toInt()
            it.height = calculatedHeight
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
                leftHeight += item.height + marginSize
            } else {
                rightList.add(item)
                rightHeight += item.height + marginSize
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
        modularDialog.postValue(modularArgs)
    }

    private fun showDetailedDialog(utxoItem: UtxosViewHolderItem) {
        val modules = mutableListOf<IDialogModule>()
        modules.add(UtxoAmountModule(utxoItem.source.value))
        if (utxoItem.showStatus) {
            modules.add(
                DetailItemModule(
                    resourceManager.getString(R.string.utxos_detailed_status),
                    resourceManager.getString(utxoItem.status!!.text),
                    utxoItem.status.textIcon
                )
            )
        }
        modules.add(DetailItemModule(resourceManager.getString(R.string.utxos_detailed_commitment), utxoItem.source.commitment))
        if (utxoItem.showMinedHeight) {
            modules.add(DetailItemModule(resourceManager.getString(R.string.utxos_detailed_block_height), utxoItem.source.minedHeight.toString()))
        }
        if (utxoItem.showLockHeight) {
            modules.add(DetailItemModule(resourceManager.getString(R.string.utxos_detailed_lock_height), utxoItem.source.lockHeight.toString()))
        }
        if (utxoItem.showDate) {
            val formattedDateTime = utxoItem.formattedDate + " " + utxoItem.formattedTime
            modules.add(DetailItemModule(resourceManager.getString(R.string.utxos_detailed_date), formattedDateTime))
        }
        if (utxoItem.selectable) {
            modules.add(
                ButtonModule(resourceManager.getString(R.string.utxos_break_button), ButtonStyle.Normal) {
                    dismissDialog.postValue(Unit)
                    split(utxoItem)
                },
            )
        }
        modules.add(ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close))
        val modularArgs = ModularDialogArgs(DialogArgs(), modules)
        modularDialog.postValue(modularArgs)
    }

    private fun showSuccessSplitDialog() = showSuccessDialog(R.string.utxos_success_split_description)

    private fun showSuccessJoinDialog() = showSuccessDialog(R.string.utxos_success_join_description)

    private fun showSuccessDialog(descriptionId: Int) {
        val modularArgs = ModularDialogArgs(
            DialogArgs {
                setSelectionState(false)
            }, listOf(
                ImageModule(R.drawable.tari_utxos_succes_popper),
                HeadModule(resourceManager.getString(R.string.utxos_success_title)),
                BodyModule(resourceManager.getString(descriptionId)),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(modularArgs)
    }
}