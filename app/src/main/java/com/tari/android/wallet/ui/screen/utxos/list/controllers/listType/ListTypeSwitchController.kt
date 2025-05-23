package com.tari.android.wallet.ui.screen.utxos.list.controllers.listType

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbar
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg

class ListTypeSwitchController(val tariToolbar: TariToolbar) {

    private var currentState: ListType? = null

    var toggleCallback: (ListType) -> Unit = {}

    init {
        set(ListType.Tile)
    }

    fun toggle() {
        set(getOppositeListType())
    }

    fun set(type: ListType) {
        currentState = type
        tariToolbar.setRightArgs(TariToolbarActionArg(icon = getListTypeIcon()) {
            toggle()
        })
        toggleCallback.invoke(currentState!!)
    }

    private fun getOppositeListType(): ListType = when (currentState) {
        ListType.Tile -> ListType.Text
        else -> ListType.Tile
    }

    private fun getListTypeIcon(): Int = when (currentState) {
        ListType.Text -> R.drawable.vector_wallet_group_cells
        else -> R.drawable.vector_wallet_group_list
    }
}