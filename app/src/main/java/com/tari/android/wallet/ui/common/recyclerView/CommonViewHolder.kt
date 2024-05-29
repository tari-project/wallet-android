package com.tari.android.wallet.ui.common.recyclerView

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class CommonViewHolder<T : CommonViewHolderItem, VB: ViewBinding>(val ui: VB) : RecyclerView.ViewHolder(ui.root) {

    var item: T? = null

    val itemClass : Class<T>?
        get() = item?.javaClass

    var clickView : View? = null

    open fun bind(item: T) {
        this.item = item
    }

    open fun onAttach() = Unit

    open fun onDetach() = Unit
}

