package com.tari.android.wallet.ui.fragment.contactBook.data

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo

class MergedContactsRepositoryBridge {

    fun updateContactsWithMergedContacts(contacts: List<ContactDto>): List<ContactDto> {

        val mergedContacts = contacts.filter { it.contactInfo is PhoneContactInfo && it.contactInfo.phoneEmojiId.isNotEmpty() }
            .map { phoneContact ->
                val phoneContactInfo = phoneContact.contactInfo as PhoneContactInfo
                val ffiContactInfo = contacts.mapNotNull { it.contactInfo as? FFIContactInfo }
                    .find { it.walletAddress.fullEmojiId == phoneContactInfo.phoneEmojiId } // todo use proper comparison
                    ?: return@map phoneContact

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