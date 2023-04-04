package com.tari.android.wallet.ui.fragment.home

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.yat.YatAdapter
import javax.inject.Inject

class HomeViewModel: CommonViewModel() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }
}