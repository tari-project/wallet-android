package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState

import android.text.SpannedString
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class EmptyStateItem(val title: SpannedString, val body: SpannedString, val image: Int, val buttonTitle: String, val action: () -> Unit) :
    CommonViewHolderItem()