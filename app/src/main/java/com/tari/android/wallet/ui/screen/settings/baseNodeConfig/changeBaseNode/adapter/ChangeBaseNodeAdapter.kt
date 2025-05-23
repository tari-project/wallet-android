package com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ChangeBaseNodeAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(BaseNodeViewHolder.getBuilder())
}