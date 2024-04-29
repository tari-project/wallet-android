package com.tari.android.wallet.ui.fragment.contact_book.address_poisoning

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import java.util.Date

data class SimilarAddressItem(
    val contact: ContactDto,
    val numberOfTransaction: Int = 0,
    val lastTransactionDate: Date? = null,
    val trusted: Boolean = false,
    val lastItem: Boolean = false,
) : CommonViewHolderItem() {
    override val viewHolderUUID
        get() = contact.uuid

    var selected: Boolean = false
}