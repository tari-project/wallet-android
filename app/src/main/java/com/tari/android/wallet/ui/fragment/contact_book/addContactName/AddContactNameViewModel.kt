package com.tari.android.wallet.ui.fragment.contact_book.addContactName

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import javax.inject.Inject

class AddContactNameViewModel : CommonViewModel() {

    val navigation = SingleLiveEvent<ContactBookNavigation>()

    val contact = MutableLiveData<ContactDto>()

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var contactBookRepository: ContactsRepository

    init {
        component.inject(this)
    }

    fun initContact(contact: ContactDto) {
        this.contact.postValue(contact)
    }

    fun onContinue(firstName: String, surname: String) {
        val contact = this.contact.value ?: return
        contactBookRepository.updateContactName(contact, firstName, surname)
        navigation.postValue(ContactBookNavigation.BackToContactBook())
    }
}