package com.tari.android.wallet.ui.screen.contactBook.details.adapter.profile

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

data class ContactProfileViewHolderItem(
    val contact: Contact,
    val onAddressClick: (address: TariWalletAddress) -> Unit,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = contact.walletAddress.fullBase58
}