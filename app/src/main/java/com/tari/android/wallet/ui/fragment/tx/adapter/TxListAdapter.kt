package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder

internal class TxListAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(TitleViewHolder.getBuilder(), TxListViewHolder.getBuilder())
}