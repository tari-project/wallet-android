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

    private var savedContacts: ContactsList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(KEY_SAVED_CONTACTS),
        type = ContactsList::class.java,
        defValue = ContactsList(),
    )

    fun getSavedContacts(): List<ContactDto> = savedContacts.map { ContactDtoSerializable.toContactDto(it) }

    @Synchronized
    fun saveContacts(list: List<ContactDto>) {
        savedContacts = ContactsList(list.map { ContactDtoSerializable.fromContactDto(it) })
    }

    fun clear() {
        savedContacts = ContactsList(emptyList())
    }

    companion object {
        const val KEY_SAVED_CONTACTS = "KEY_SAVED_CONTACTS"
    }
}