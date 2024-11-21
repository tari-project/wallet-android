package com.tari.android.wallet.ui.screen.contactBook.details.adapter.profile

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.data.contacts.model.ContactDto

data class ContactProfileViewHolderItem(
    val contactDto: ContactDto,
    val onAddressClick: (address: TariWalletAddress) -> Unit,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = contactDto.uuid
}