package com.tari.android.wallet.ui.fragment.contactBook.details

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

object ContactDetailsModel {
    data class UiState(
        val contact: ContactDto,
        val list: List<CommonViewHolderItem> = emptyList(),
    )
}