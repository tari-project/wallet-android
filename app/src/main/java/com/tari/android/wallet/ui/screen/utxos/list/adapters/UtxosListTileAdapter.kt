package com.tari.android.wallet.ui.screen.utxos.list.adapters

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class UtxosListTileAdapter : CommonAdapter<UtxosViewHolderItem>() {

    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(UtxosTileListViewHolder.getBuilder())
}