package com.tari.android.wallet.ui.fragment.contactBook.link.adapter.link_header

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class ContactLinkHeaderViewHolderItem(val searchAction: (String) -> Unit, val walletAddress: TariWalletAddress) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "ContactLinkHeaderViewHolderItem" + walletAddress.fullBase58
}

