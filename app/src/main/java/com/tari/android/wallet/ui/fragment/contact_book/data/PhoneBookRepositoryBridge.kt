package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import contacts.core.Contacts
import contacts.core.Fields
import contacts.core.entities.NewOptions
import contacts.core.equalTo
import contacts.core.util.names
import contacts.core.util.setName
import contacts.core.util.setOptions

class PhoneBookRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    private val context: Context,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val phoneContactsSource = Contacts(context)

    internal suspend fun updateContactListWithPhoneBook() {
        if (!contactsRepository.contactPermissionGranted) return

        logger.i("ContactsRepository: Loading contacts from phone book")
        val phoneContacts = getPhoneContacts()

        contactsRepository.updateContactList { contacts ->
            phoneContacts.forEach { phoneContact ->
                val existingContact = contacts.firstOrNull { it.getPhoneDto()?.id == phoneContact.id }
                if (existingContact == null) {
                    contacts.add(ContactDto(phoneContact.toPhoneContactDto()))
                } else {
                    existingContact.getPhoneDto()?.let {
                        it.avatar = phoneContact.avatar
                        it.firstName = phoneContact.firstName
                        it.surname = phoneContact.surname
                        it.displayName = phoneContact.displayName
                        it.isFavorite = phoneContact.isFavorite
//                                it.yat = phoneContact.yat
//                                it.phoneEmojiId = phoneContact.emojiId
                    }
                }
            }
        }
    }

    internal suspend fun updateToPhoneBook() {
        if (contactsRepository.contactPermissionGranted) {
            logger.i("ContactsRepository: Saving updates to contact book")
            try {
                contactsRepository.updateContactList(silently = true) { contacts ->
                    val phoneContacts = contacts.mapNotNull { it.getPhoneDto() }.filter { it.shouldUpdate }

                    for (item in phoneContacts) {
                        val contact = ContactsRepository.PhoneContact(
                            id = item.id,
                            firstName = item.firstName,
                            surname = item.surname,
                            displayName = item.displayName,
                            yat = item.yat,
                            emojiId = item.phoneEmojiId,
                            avatar = item.avatar,
                            isFavorite = item.isFavorite,
                        )
                        saveNamesToPhoneBook(contact)
                        saveStarredToPhoneBook(contact)
//                                saveCustomFieldsToPhoneBook(contact) // todo why it's  commented? Should we store Yat and EmojiID custom fields in the phone book?
                        item.shouldUpdate = false
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    internal suspend fun deleteFromContactBook(contact: PhoneContactDto) {
        logger.i("ContactsRepository: Deleting contact from contact book")
        phoneContactsSource.delete().contactsWithId(contact.id.toLong()).commit()
    }

    private fun saveNamesToPhoneBook(contact: ContactsRepository.PhoneContact) {
        val phoneContact = phoneContactsSource.query().include(Fields.all()).where { Contact.Id equalTo contact.id }.find().firstOrNull()

        val updatedContact = phoneContact?.mutableCopy {
            val name = this.names().firstOrNull()?.redactedCopy()?.apply {
                this.givenName = contact.firstName
                this.familyName = contact.surname
                this.displayName = contact.displayName
            }

            setName(name)
        }

        updatedContact?.let {
            phoneContactsSource.update()
                .contacts(it)
                .commit()
        }
    }

    private fun getPhoneContacts() = phoneContactsSource.query().include(Fields.all()).find()
        .map {
            val name = it.names().firstOrNull()
            ContactsRepository.PhoneContact(
                id = it.id.toString(),
                firstName = name?.givenName.orEmpty(),
                surname = name?.familyName.orEmpty(),
                displayName = name?.displayName.orEmpty(),
                yat = "",
                emojiId = "",
                avatar = it.photoUri?.toString().orEmpty(),
                isFavorite = it.options?.starred ?: false,
            )
        }.toMutableList()


    private fun saveStarredToPhoneBook(contact: ContactsRepository.PhoneContact) {
        val phoneContact = phoneContactsSource.query().include(Fields.all()).where { Contact.Id equalTo contact.id }.find().firstOrNull()

        val updatedContact = phoneContact?.mutableCopy {
            if (options == null) {
                setOptions(NewOptions(starred = contact.isFavorite))
            } else {
                val copy = options!!.redactedCopy()
                copy.starred = contact.isFavorite
                setOptions(copy)
            }
        }

        updatedContact?.let {
            phoneContactsSource.update()
                .contacts(it)
                .commit()
        }
    }

    private fun saveCustomFieldsToPhoneBook(contact: ContactsRepository.PhoneContact) {
        runCatching {
            val operations = arrayListOf<ContentProviderOperation>()

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contact.id.toInt())
                .withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/com.tari.android.wallet.yat")
                .withValue("data1", contact.yat)
                .build().apply { operations.add(this) }

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contact.id.toInt())
                .withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/com.tari.android.wallet.emojiId")
                .withValue("data1", contact.emojiId)
                .build().apply { operations.add(this) }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        }
    }
}
