package com.tari.android.wallet.ui.screen.contactBook.obsolete.link.adapter.linkHeader

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class ContactLinkHeaderViewHolderItem(val searchAction: (String) -> Unit, val walletAddress: TariWalletAddress) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "ContactLinkHeaderViewHolderItem" + walletAddress.fullBase58
}

