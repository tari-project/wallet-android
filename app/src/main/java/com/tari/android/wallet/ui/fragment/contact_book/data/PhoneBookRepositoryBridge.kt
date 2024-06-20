package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactInfo
import contacts.core.Contacts
import contacts.core.Fields
import contacts.core.entities.NewOptions
import contacts.core.equalTo
import contacts.core.util.names
import contacts.core.util.setName
import contacts.core.util.setOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneBookRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    private val context: Context,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val phoneContactsSource = Contacts(context)

    internal suspend fun updateContactListWithPhoneBook(contacts: List<ContactDto>): List<ContactDto> {
        if (!contactsRepository.contactPermissionGranted) return contacts

        logger.i("Contacts repository event: Loading contacts from phone book")

        val phoneContacts = getPhoneContacts()

        return contacts
            // update existing contacts
            .map { contact ->
                phoneContacts.firstOrNull { it.id == contact.getPhoneContactInfo()?.id }?.let { phoneContact ->
                    when (contact.contactInfo) {
                        is PhoneContactInfo -> contact.copy(
                            contactInfo = contact.contactInfo.copy(
                                id = phoneContact.id,
                                avatar = phoneContact.avatar,
                                firstName = phoneContact.firstName,
                                lastName = phoneContact.lastName,
                                displayName = phoneContact.displayName,
                                phoneEmojiId = phoneContact.phoneEmojiId,
                                isFavorite = phoneContact.isFavorite,
                            )
                        )

                        is MergedContactInfo -> contact.copy(
                            contactInfo = contact.contactInfo.copy(
                                phoneContactInfo = contact.contactInfo.phoneContactInfo.copy(
                                    id = phoneContact.id,
                                    avatar = phoneContact.avatar,
                                    firstName = phoneContact.firstName,
                                    lastName = phoneContact.lastName,
                                    displayName = phoneContact.displayName,
                                    phoneEmojiId = phoneContact.phoneEmojiId,
                                    isFavorite = phoneContact.isFavorite,
                                )
                            )
                        )

                        else -> contact
                    }
                } ?: contact // do nothing if phone contact is not exist
            }
            // add new contacts
            .plus(
                phoneContacts.filter { phoneContact -> contacts.none { it.getPhoneContactInfo()?.id == phoneContact.id } }
                    .map { ContactDto(it) }
            )
    }

    internal suspend fun updateToPhoneBook(contacts: List<ContactDto>): List<ContactDto> {
        if (!contactsRepository.contactPermissionGranted) return contacts

        logger.i("Contacts repository event: Saving updates to contact book")
        try {
            return contacts.map { contact ->
                contact.getPhoneContactInfo()?.takeIf { it.shouldUpdate }?.let { phoneContact ->
                    saveNamesToPhoneBook(phoneContact)
                    saveStarredToPhoneBook(phoneContact)
//                                saveCustomFieldsToPhoneBook(phoneContact) // todo why it's  commented? Should we store Yat and EmojiID custom fields in the phone book?

                    when (contact.contactInfo) {
                        is PhoneContactInfo -> {
                            contact.copy(
                                contactInfo = contact.contactInfo.copy(
                                    shouldUpdate = false
                                )
                            )
                        }

                        is MergedContactInfo -> {
                            contact.copy(
                                contactInfo = contact.contactInfo.copy(
                                    phoneContactInfo = contact.contactInfo.phoneContactInfo.copy(
                                        shouldUpdate = false
                                    )
                                )
                            )
                        }

                        else -> contact
                    }
                } ?: contact
            }
        } catch (e: Throwable) {
            logger.e("Contacts repository event: Failed to save updates to contact book")
            e.printStackTrace()
            return contacts
        }
    }

    internal suspend fun deleteFromContactBook(contact: PhoneContactInfo) = withContext(Dispatchers.IO) {
        logger.i("Contacts repository event: Deleting contact from contact book")
        phoneContactsSource.delete().contactsWithId(contact.id.toLong()).commit()
    }

    private suspend fun saveNamesToPhoneBook(contact: PhoneContactInfo) = withContext(Dispatchers.IO) {
        val phoneContact = phoneContactsSource.query().include(Fields.all()).where { Contact.Id equalTo contact.id }.find().firstOrNull()

        val updatedContact = phoneContact?.mutableCopy {
            val name = this.names().firstOrNull()?.redactedCopy()?.apply {
                this.givenName = contact.firstName
                this.familyName = contact.lastName
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

    private suspend fun getPhoneContacts(): List<PhoneContactInfo> = withContext(Dispatchers.IO) {
        phoneContactsSource.query().include(Fields.all()).find()
            .map {
                val name = it.names().firstOrNull()
                PhoneContactInfo(
                    id = it.id.toString(),
                    firstName = name?.givenName.orEmpty(),
                    lastName = name?.familyName.orEmpty(),
                    displayName = name?.displayName.orEmpty(),
                    phoneEmojiId = "",
                    avatar = it.photoUri?.toString().orEmpty(),
                    isFavorite = it.options?.starred ?: false,
                )
            }
    }


    private suspend fun saveStarredToPhoneBook(contact: PhoneContactInfo) = withContext(Dispatchers.IO) {
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

    private suspend fun saveCustomFieldsToPhoneBook(contactId: Int, yat: String, emojiId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val operations = arrayListOf<ContentProviderOperation>()

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactId)
                .withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/com.tari.android.wallet.yat")
                .withValue("data1", yat)
                .build().apply { operations.add(this) }

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactId)
                .withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/com.tari.android.wallet.emojiId")
                .withValue("data1", emojiId)
                .build().apply { operations.add(this) }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        }
    }
}
