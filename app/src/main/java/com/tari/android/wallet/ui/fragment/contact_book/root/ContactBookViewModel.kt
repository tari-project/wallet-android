package com.tari.android.wallet.ui.fragment.contact_book.root

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }
}