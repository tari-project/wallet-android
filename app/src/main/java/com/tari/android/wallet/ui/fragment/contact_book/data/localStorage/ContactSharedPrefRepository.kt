package com.tari.android.wallet.ui.fragment.contact_book.data.localStorage

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSharedPrefRepository @Inject constructor(
    networkRepository: NetworkRepository,
    val sharedPrefs: SharedPreferences
) : CommonRepository(networkRepository) {

    var savedContacts: ContactsList? by SharedPrefGsonDelegate(sharedPrefs, formatKey(KEY_SAVED_CONTACTS), ContactsList::class.java, ContactsList())

    fun clear() {
        savedContacts = null
    }

    companion object {
        const val KEY_SAVED_CONTACTS = "KEY_SAVED_CONTACTS"
    }
}