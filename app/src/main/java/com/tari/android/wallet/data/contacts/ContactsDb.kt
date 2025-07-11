package com.tari.android.wallet.data.contacts

import com.tari.android.wallet.model.Base58
import io.paperdb.Paper
import javax.inject.Inject
import javax.inject.Singleton

private const val CONTACTS_DB_KEY = "contacts_db"

@Singleton
class ContactsDb @Inject constructor() {

    suspend fun loadAllContacts(): List<ContactDto> = Paper.book().read(CONTACTS_DB_KEY) ?: emptyList()

    suspend fun saveContacts(contacts: List<ContactDto>) {
        Paper.book().write(CONTACTS_DB_KEY, contacts)
    }

    suspend fun upsertContact(contact: ContactDto) {
        val contacts = loadAllContacts().toMutableList()
        val existingContactIndex = contacts.indexOfFirst { it.walletAddressBase58 == contact.walletAddressBase58 }
        if (existingContactIndex != -1) {
            contacts[existingContactIndex] = contact
        } else {
            contacts.add(0, contact)
        }
        saveContacts(contacts.filter { !it.alias.isNullOrBlank() })
    }

    suspend fun upsertContactList(contacts: List<ContactDto>) {
        val allContacts = loadAllContacts().toMutableList()
        contacts.forEach { contact ->
            val existingContactIndex = allContacts.indexOfFirst { it.walletAddressBase58 == contact.walletAddressBase58 }
            if (existingContactIndex != -1) {
                allContacts[existingContactIndex] = contact
            } else {
                allContacts.add(0, contact)
            }
        }
        saveContacts(allContacts.filter { !it.alias.isNullOrBlank() })
    }

    suspend fun deleteContact(walletAddressBase58: Base58) {
        val contacts = loadAllContacts().toMutableList()
        contacts.removeAll { it.walletAddressBase58 == walletAddressBase58 }
        saveContacts(contacts)
    }

    data class ContactDto(
        val alias: String,
        val walletAddressBase58: Base58,
    )
}