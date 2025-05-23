package com.tari.android.wallet.ui.screen.settings.allSettings.about.list

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class TariIconsAdapter : CommonAdapter<TariIconViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(TariIconViewHolder.getBuilder())
}