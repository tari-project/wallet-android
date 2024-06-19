package com.tari.android.wallet.data.sharedPrefs.contacts

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactPrefRepository @Inject constructor(
    networkRepository: NetworkPrefRepository,
    val sharedPrefs: SharedPreferences
) : CommonRepository(networkRepository) {

    private var _savedContacts: ContactList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(KEY_SAVED_CONTACTS),
        type = ContactList::class.java,
        defValue = ContactList(),
    )
    var savedContacts: List<ContactDto>
        // Return empty list if failed to get contacts because of old data format
        get() = runCatching { _savedContacts }.getOrDefault(emptyList())
        set(value) {
            _savedContacts = ContactList(value)
        }

    fun clear() {
        _savedContacts = ContactList(emptyList())
    }

    companion object {
        const val KEY_SAVED_CONTACTS = "KEY_SAVED_CONTACTS"
    }
}

class ContactList(contacts: List<ContactDto>) : ArrayList<ContactDto>(contacts) {
    constructor() : this(emptyList())
}