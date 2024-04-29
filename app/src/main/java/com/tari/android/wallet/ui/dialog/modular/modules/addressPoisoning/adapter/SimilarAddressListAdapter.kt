package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SimilarAddressListAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(SimilarAddressItemViewHolder.getBuilder())
}