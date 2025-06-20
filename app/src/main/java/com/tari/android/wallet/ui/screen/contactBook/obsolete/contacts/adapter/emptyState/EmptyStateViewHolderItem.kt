package com.tari.android.wallet.ui.screen.contactBook.obsolete.contacts.adapter.emptyState

import android.text.SpannedString
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

data class EmptyStateViewHolderItem(
    val title: SpannedString,
    val body: SpannedString,
    val image: Int,
    val buttonTitle: String,
    val action: () -> Unit,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = title.toString()
}