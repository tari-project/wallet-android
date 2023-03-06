package com.tari.android.wallet.ui.fragment.contact_book.data.localStorage

import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import java.io.Serializable

class ContactsList() : ArrayList<ContactDto>(), Serializable {
    constructor(list: List<ContactDto>) : this() {
        this.addAll(list)
    }
}

fun ContactsList?.orEmpty(): ContactsList = this ?: ContactsList()