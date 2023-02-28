package com.tari.android.wallet.ui.fragment.contact_book.addContactName

import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.executeWithError
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.send.addRecepient.recipientList.RecipientHeaderItem
import com.tari.android.wallet.ui.fragment.send.addRecepient.recipientList.RecipientViewHolderItem
import com.tari.android.wallet.util.Build
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.yat.YatAdapter
import com.tari.android.wallet.yat.YatUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject

class AddContactNameViewModel : CommonViewModel() {

    private val _navigation: SingleLiveEvent<ContactBookNavigation> = SingleLiveEvent()
    val navigation: LiveData<ContactBookNavigation> = _navigation

    val contact = MutableLiveData<IContact>()

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var contactBookRepository: ContactsRepository

    init {
        component.inject(this)
    }

    fun initContact(contact: IContact) {
        this.contact.postValue(contact)
    }

    fun onContinue(name: String) {
        val contact = this.contact.value ?: return
        val ffiContact = contact as FFIContactDto
        ffiContact.localAlias = name
        contactBookRepository.addContact(ffiContact)
        _navigation.postValue(ContactBookNavigation.ToFinalizeAddingContact)
    }
}