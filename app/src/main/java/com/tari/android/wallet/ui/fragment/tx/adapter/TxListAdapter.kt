package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.ui.common.recyclerView.*
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder

class TxListAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(TitleViewHolder.getBuilder(), TxListViewHolder.getBuilder())
}