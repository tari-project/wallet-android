package com.tari.android.wallet.ui.common.recyclerView

import androidx.recyclerview.widget.DiffUtil

class DefaultDiffCallback<T : CommonViewHolderItem> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem.viewHolderUUID == newItem.viewHolderUUID

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem

    override fun getChangePayload(oldItem: T, newItem: T): Any = Any()
}