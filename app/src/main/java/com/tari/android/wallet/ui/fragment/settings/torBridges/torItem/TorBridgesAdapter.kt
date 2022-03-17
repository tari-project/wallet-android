package com.tari.android.wallet.ui.fragment.settings.torBridges.torItem

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class TorBridgesAdapter : CommonAdapter<TorBridgeViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(TorBridgesViewHolder.getBuilder())
}