package com.tari.android.wallet.ui.fragment.settings.networkSelection.networkItem

import com.tari.android.wallet.databinding.ItemNetworkBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.setVisible

class NetworkTariViewHolder(view: ItemNetworkBinding) : CommonViewHolder<NetworkViewHolderItem, ItemNetworkBinding>(view) {

    override fun bind(item: NetworkViewHolderItem) {
        super.bind(item)

        ui.tvName.text = item.network.network.displayName

        ui.done.setVisible(item.network.network == item.currentNetwork)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemNetworkBinding::inflate, NetworkViewHolderItem::class.java) { NetworkTariViewHolder(it as ItemNetworkBinding) }
    }
}