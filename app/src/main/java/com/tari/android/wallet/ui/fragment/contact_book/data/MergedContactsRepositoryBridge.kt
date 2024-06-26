package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactInfo

class MergedContactsRepositoryBridge {

    fun updateContactsWithMergedContacts(contacts: List<ContactDto>): List<ContactDto> {

        val mergedContacts = contacts.filter { it.contactInfo is PhoneContactInfo && it.contactInfo.phoneEmojiId.isNotEmpty() }
            .map { phoneContact ->
                val phoneContactInfo = phoneContact.contactInfo as PhoneContactInfo
                val ffiContactInfo = contacts.mapNotNull { it.contactInfo as? FFIContactInfo }
                    .find { it.walletAddress.emojiId == phoneContactInfo.phoneEmojiId }
                    ?: error("FFI contact with address ${phoneContact.walletAddress} not found")

                phoneContact.copy(
                    contactInfo = MergedContactInfo(
                        phoneContactInfo = phoneContactInfo,
                        ffiContactInfo = ffiContactInfo,
                        isFavorite = phoneContactInfo.isFavorite || ffiContactInfo.isFavorite,
                    )
                )
            }


        return contacts
            .filterNot { it.contactInfo is PhoneContactInfo && it.contactInfo.phoneEmojiId.isNotEmpty() }
            .filterNot { it.contactInfo is FFIContactInfo && mergedContacts.map { c -> c.getFFIContactInfo() }.contains(it.contactInfo) }
            .plus(mergedContacts)
    }
}