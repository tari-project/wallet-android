package com.tari.android.wallet.data.contacts

import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariWalletAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    private val contactsDb: ContactsDb,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _contactList: MutableStateFlow<List<Contact>> = MutableStateFlow(emptyList())
    val contactList = _contactList.asStateFlow()

    init {
        applicationScope.launch {
            refreshContactList()
        }
    }

    /**
     * Update contact info or add a new contact if it does not exist
     */
    suspend fun updateContactInfo(
        contactToUpdate: Contact,
        alias: String?,
    ): Contact {
        val newContact = contactToUpdate.copy(alias = alias?.trim())

        if (newContact.alias == null) {
            contactsDb.deleteContact(contactToUpdate.walletAddress.fullBase58)
        } else {
            contactsDb.upsertContact(
                ContactsDb.ContactDto(
                    alias = newContact.alias,
                    walletAddressBase58 = contactToUpdate.walletAddress.fullBase58,
                ),
            )
        }

        refreshContactList()

        return newContact
    }

    suspend fun addContact(contact: Contact) {
        contactsDb.upsertContact(
            ContactsDb.ContactDto(
                alias = contact.alias.orEmpty(),
                walletAddressBase58 = contact.walletAddress.fullBase58,
            ),
        )
        refreshContactList()
    }

    suspend fun addContactList(contacts: List<Contact>) {
        contactsDb.upsertContactList(contacts.map { contact ->
            ContactsDb.ContactDto(
                alias = contact.alias.orEmpty(),
                walletAddressBase58 = contact.walletAddress.fullBase58,
            )
        })
        refreshContactList()
    }

    suspend fun deleteContact(contact: Contact) {
        contactsDb.deleteContact(contact.walletAddress.fullBase58)
        refreshContactList()
    }

    fun findOrCreateContact(address: TariWalletAddress): Contact =
        contactList.value.firstOrNull { it.walletAddress == address } ?: Contact(walletAddress = address)

    private suspend fun refreshContactList() {
        _contactList.value = contactsDb.loadAllContacts().map { contactDto ->
            Contact(
                alias = contactDto.alias,
                walletAddress = TariWalletAddress.fromBase58(contactDto.walletAddressBase58),
            )
        }
    }
}