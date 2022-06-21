package com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering

import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.tari.android.wallet.R

class OrderingController(private val valueView: View, private val dateView: View, private val directionView: ImageView) {

    private val sortingTypeList = listOf(valueView, dateView)
    private var currentDirection: OrderDirection? = null
    private var currentType: OrderType? = null

    var toggleDirectionCallback: (OrderDirection) -> Unit = {}
    var toggleTypeCallback: (OrderType) -> Unit = {}

    init {
        valueView.setOnClickListener { toggleType(OrderType.ByValue) }
        dateView.setOnClickListener { toggleType(OrderType.ByDate) }
        directionView.setOnClickListener { toggleDirection() }
    }

    fun toggleType(type: OrderType) {
        if (currentType != type) {
            currentType = type
            toggleTypeCallback.invoke(type)
            sortingTypeList.forEach { it.background = null }
            getTypeView().background = ContextCompat.getDrawable(directionView.context, R.drawable.background_utxos_list_sorting_selector_checked)
        }
    }

    fun toggleDirection(direction: OrderDirection? = null) {
        val newDirection = direction ?: getOppositeDirection()
        if (newDirection != currentDirection) {
            currentDirection = newDirection
            toggleDirectionCallback.invoke(currentDirection!!)
            directionView.setImageResource(getDirectionRes())
        }
    }

    private fun getDirectionRes(): Int = when (currentDirection) {
        OrderDirection.Anc -> R.drawable.ic_wallet_sorting_asc
        else -> R.drawable.ic_wallet_sorting_desc
    }

    private fun getOppositeDirection() = when(currentDirection) {
        OrderDirection.Anc -> OrderDirection.Desc
        else -> OrderDirection.Anc
    }

    private fun getTypeView(): View = when (currentType) {
        OrderType.ByValue -> valueView
        else -> dateView
    }
}