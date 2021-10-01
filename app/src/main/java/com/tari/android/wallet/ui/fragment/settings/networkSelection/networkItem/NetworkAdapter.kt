package com.tari.android.wallet.ui.fragment.settings.networkSelection.networkItem

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class NetworkAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(NetworkTariViewHolder.getBuilder())
}