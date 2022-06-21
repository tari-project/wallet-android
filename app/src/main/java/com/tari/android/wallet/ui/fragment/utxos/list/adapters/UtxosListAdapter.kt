package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class UtxosListAdapter : CommonAdapter<UtxosViewHolderItem>() {

    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(UtxosTextListViewHolder.getBuilder())
}

