package com.tari.android.wallet.data.contacts

import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.data.contacts.model.FFIContactInfo
import com.tari.android.wallet.data.contacts.model.MergedContactInfo
import com.tari.android.wallet.data.contacts.model.PhoneContactInfo

class MergedContactsRepositoryBridge {

    fun updateContactsWithMergedContacts(contacts: List<ContactDto>): List<ContactDto> {

        val mergedContacts = contacts.filter { it.contactInfo is PhoneContactInfo && !it.contactInfo.phoneEmojiId.isNullOrBlank() }
            .map { phoneContact ->
                val phoneContactInfo = phoneContact.contactInfo as PhoneContactInfo
                val ffiContactInfo = contacts.mapNotNull { it.contactInfo as? FFIContactInfo }
                    .find { it.walletAddress.fullEmojiId == phoneContactInfo.phoneEmojiId }
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
            .filterNot { it.contactInfo is PhoneContactInfo && !it.contactInfo.phoneEmojiId.isNullOrBlank() }
            .filterNot { it.contactInfo is FFIContactInfo && mergedContacts.map { c -> c.getFFIContactInfo() }.contains(it.contactInfo) }
            .plus(mergedContacts)
    }
}