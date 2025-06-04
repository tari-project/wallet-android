package com.tari.android.wallet.data.contacts.obsolete

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.contacts.obsolete.model.ContactDto
import com.tari.android.wallet.data.contacts.obsolete.model.MergedContactInfo
import com.tari.android.wallet.data.contacts.obsolete.model.PhoneContactInfo
import com.tari.android.wallet.model.EmojiId
import contacts.core.Contacts
import contacts.core.Fields
import contacts.core.entities.MutableContact
import contacts.core.entities.NewNote
import contacts.core.entities.NewOptions
import contacts.core.entities.Note
import contacts.core.equalTo
import contacts.core.util.names
import contacts.core.util.notes
import contacts.core.util.setName
import contacts.core.util.setNote
import contacts.core.util.setOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneBookRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    context: Context,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val phoneContactsSource = Contacts(context)

    internal suspend fun updateContactsWithPhoneBook(contacts: List<ContactDto>): List<ContactDto> {
        if (!contactsRepository.contactPermissionGranted) return contacts

        logger.i("Contacts repository event: Loading contacts from phone book")

        val phoneContacts = getPhoneContacts()

        return contacts
            // update existing contacts
            .map { contact ->
                phoneContacts.firstOrNull { it.id == contact.getPhoneContactInfo()?.id }?.let { phoneContact ->
                    when (contact.contactInfo) {
                        is PhoneContactInfo -> contact.copy(contactInfo = phoneContact)
                        is MergedContactInfo -> contact.copy(contactInfo = contact.contactInfo.copy(phoneContactInfo = phoneContact))
                        else -> contact
                    }
                } ?: contact // do nothing if phone contact does not exist
            }
            // add new contacts
            .plus(
                phoneContacts.filter { phoneContact -> contacts.none { it.getPhoneContactInfo()?.id == phoneContact.id } }
                    .map { ContactDto(it) }
            )
    }

    internal suspend fun updateToPhoneBook(contacts: List<ContactDto>) {
        if (!contactsRepository.contactPermissionGranted) return

        logger.i("Contacts repository event: Saving updates to contact book")
        try {
            contacts.map { contact ->
                contact.getPhoneContactInfo()?.takeIf { it.shouldUpdate }?.let { phoneContact ->
                    saveNamesToPhoneBook(phoneContact)
                    saveStarredToPhoneBook(phoneContact)
                    saveCustomFieldsToPhoneBook(phoneContact)
                }
            }
        } catch (e: Throwable) {
            logger.e("Contacts repository event: Failed to save updates to contact book")
            e.printStackTrace()
        }
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

        updatePhoneContact(updatedContact)
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

        updatePhoneContact(updatedContact)
    }

    private suspend fun saveCustomFieldsToPhoneBook(contact: PhoneContactInfo) = withContext(Dispatchers.IO) {
        val phoneContact = phoneContactsSource.query().include(Fields.all()).where { Contact.Id equalTo contact.id }.find().firstOrNull()

        val updatedContact = phoneContact?.mutableCopy {
            setNote(generateCustomFields(contact.phoneYat.orEmpty(), contact.phoneEmojiId.orEmpty()))
        }

        updatePhoneContact(updatedContact)
    }

    private fun updatePhoneContact(updatedContact: MutableContact?) {
        val result = updatedContact?.let {
            phoneContactsSource.update()
                .contacts(it)
                .commit()
        }

        if (result?.isSuccessful == false) {
            logger.i(
                "Failed to save contact to phone book: $updatedContact with error: $result" +
                        "\nProbably the android.Manifest.permission.WRITE_CONTACTS permission is not granted."
            )
        }
    }

    private fun generateCustomFields(yat: String, emojiId: EmojiId): NewNote? {
        val formattedYat = if (yat.isNotEmpty()) "Yat: $yat" else ""
        val formattedEmojiId = if (emojiId.isNotEmpty()) "Tari address: $emojiId" else ""

        return NewNote(
            note = listOf(formattedYat, formattedEmojiId).filter { it.isNotEmpty() }.joinToString("\n"),
        ).takeIf { it.note?.isNotEmpty() == true }
    }

    private fun parseCustomFields(note: Note?): Pair<String?, String?> {
        return note?.note?.split("\n")
            ?.map { it.split(": ") }
            ?.associate { it.first() to it.last() }
            ?.let { it["Yat"] to it["Tari address"] } ?: (null to null)
    }

    private suspend fun getPhoneContacts(): List<PhoneContactInfo> = withContext(Dispatchers.IO) {
        phoneContactsSource.query().include(Fields.all()).find()
            .map { contact ->
                val name = contact.names().firstOrNull()
                val (yat, emojiId) = parseCustomFields(contact.notes().firstOrNull())

                PhoneContactInfo(
                    id = contact.id.toString(),
                    firstName = name?.givenName.orEmpty(),
                    lastName = name?.familyName.orEmpty(),
                    displayName = name?.displayName.orEmpty(),
                    phoneEmojiId = emojiId,
                    phoneYat = yat,
                    avatar = contact.photoUri?.toString().orEmpty(),
                    isFavorite = contact.options?.starred ?: false,
                )
            }
            .filter { it.displayName.isNotEmpty() || it.firstName.isNotEmpty() || it.lastName.isNotEmpty() }
    }

    internal suspend fun deleteFromContactBook(contact: PhoneContactInfo) = withContext(Dispatchers.IO) {
        logger.i("Contacts repository event: Deleting contact from contact book")

        phoneContactsSource.delete().contactsWithId(contact.id.toLong()).commit()
    }
}
