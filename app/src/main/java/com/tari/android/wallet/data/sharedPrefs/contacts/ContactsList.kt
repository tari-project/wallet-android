package com.tari.android.wallet.data.sharedPrefs.contacts

import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.IContact
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import java.io.Serializable

class ContactsList(contacts: List<ContactDtoSerializable>) : ArrayList<ContactDtoSerializable>(contacts), Serializable {
    constructor() : this(emptyList())
}

class ContactDtoSerializable() : Serializable {

    var lastUsedDate: SerializableTime? = null
    var uuid: String = ""
    var ffiContactDto: FFIContactDto? = null
    var phoneBookDto: PhoneContactDto? = null
    var mergedDto: MergedContactDto? = null

    constructor(contactDto: ContactDto) : this() {
        lastUsedDate = contactDto.lastUsedDate
        uuid = contactDto.uuid
        when (contactDto.contact) {
            is FFIContactDto -> ffiContactDto = contactDto.getFFIDto()
            is PhoneContactDto -> phoneBookDto = contactDto.getPhoneDto()
            is MergedContactDto -> mergedDto = contactDto.getMergedDto()
        }
    }

    fun getDto(): IContact = ffiContactDto ?: phoneBookDto ?: mergedDto!!

    companion object {
        fun toContactDto(contactDtoSerializable: ContactDtoSerializable): ContactDto =
            ContactDto(contactDtoSerializable.getDto(), contactDtoSerializable.uuid, contactDtoSerializable.lastUsedDate)

        fun fromContactDto(contactDto: ContactDto): ContactDtoSerializable = ContactDtoSerializable(contactDto)
    }
}