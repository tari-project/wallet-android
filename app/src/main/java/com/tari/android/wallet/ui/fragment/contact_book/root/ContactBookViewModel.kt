package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val sharedState = MutableLiveData(false)

    init {
        component.inject(this)
    }

    fun setSharedState(state: Boolean) {
        sharedState.value = state
    }

    fun shareSelectedContacts() {
        //todo
        setSharedState(false)
    }
}