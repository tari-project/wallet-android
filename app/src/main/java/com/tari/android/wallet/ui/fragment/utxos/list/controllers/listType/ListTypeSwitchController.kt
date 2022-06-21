package com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType

import com.tari.android.wallet.ui.fragment.utxos.list.controllers.CheckedController

class ListTypeSwitchController(val tile: CheckedController, val text: CheckedController) {

    private var allViews = listOf(tile, text)
    private var currentState: ListType? = null

    var toggleCallback: (ListType) -> Unit = {}

    init {
        tile.view.setOnClickListener { toggle(ListType.Tile) }
        text.view.setOnClickListener { toggle(ListType.Text) }
    }

    fun toggle(type: ListType) {
        if (type != currentState) {
            currentState = type
            allViews.forEach { it.setChecked(false) }
            getCurrentView().setChecked(true)
            toggleCallback.invoke(currentState!!)
        }
    }

    private fun getCurrentView(): CheckedController {
        return when (currentState) {
            ListType.Tile -> tile
            else -> text
        }
    }
}