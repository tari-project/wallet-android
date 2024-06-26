package com.tari.android.wallet.ui.fragment.contactBook.details.adapter.contactType

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class ContactTypeViewHolderItem(val type: String, val icon: Int) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "ContactTypeViewHolderItem$type"
}