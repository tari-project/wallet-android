package com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType

import android.widget.ImageView
import com.tari.android.wallet.R

class ListTypeSwitchController(val icon: ImageView) {

    private var currentState: ListType? = null

    var toggleCallback: (ListType) -> Unit = {}

    init {
        icon.setOnClickListener { toggle() }
    }

    fun toggle() {
        set(getOppositeListType())
    }

    fun set(type: ListType) {
        currentState = type
        icon.setImageResource(getListTypeIcon())
        toggleCallback.invoke(currentState!!)
    }

    private fun getOppositeListType() : ListType = when(currentState) {
        ListType.Tile -> ListType.Text
        else -> ListType.Tile
    }

    private fun getListTypeIcon() : Int = when(currentState) {
        ListType.Text -> R.drawable.vector_wallet_group_cells
        else -> R.drawable.vector_wallet_group_list
    }
}