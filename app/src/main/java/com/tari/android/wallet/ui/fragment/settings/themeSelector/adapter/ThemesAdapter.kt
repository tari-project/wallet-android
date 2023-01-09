package com.tari.android.wallet.ui.fragment.settings.themeSelector.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ThemesAdapter : CommonAdapter<ThemeViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(ThemesViewHolder.getBuilder())
}