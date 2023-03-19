package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import java.io.Serializable
import java.util.UUID

class ContactDto(
    var contact: IContact,
    var uuid: String = UUID.randomUUID().toString(),
    var lastUsedDate: SerializableTime? = null,
) : Serializable {
    fun filtered(text: String): Boolean = contact.filtered(text)

    fun getContactActions(): List<ContactAction> {
        val actions = mutableListOf<ContactAction>()

        if (contact is FFIContactDto || contact is MergedContactDto) {
            actions.add(ContactAction.Send)
        }

        if (contact is FFIContactDto) {
            actions.add(ContactAction.Link)
        }

        if (contact is MergedContactDto) {
            actions.add(ContactAction.Unlink)
        }

        actions.add(ContactAction.OpenProfile)
        actions.add(ContactAction.EditName)

        if (contact.isFavorite) {
            actions.add(ContactAction.ToUnFavorite)
        } else {
            actions.add(ContactAction.ToFavorite)
        }

        actions.add(ContactAction.Delete)

        return actions
    }

    fun getTypeName(): Int = when (contact) {
        is YatContactDto -> R.string.contact_book_type_yat
        is FFIContactDto -> R.string.contact_book_type_ffi
        is MergedContactDto -> R.string.contact_book_type_merged
        else -> R.string.contact_book_type_contact_book
    }

    fun getTypeIcon(): Int = when (contact) {
        is YatContactDto -> R.drawable.vector_yat_logo
        is FFIContactDto -> R.drawable.vector_gem
        is MergedContactDto -> R.drawable.vector_contact_type_link
        else -> R.drawable.vector_contact_book_type
    }

    fun getYatDto(): YatContactDto? = (contact as? YatContactDto) ?: (contact as? MergedContactDto)?.ffiContactDto as? YatContactDto

    fun getFFIDto(): FFIContactDto? = (contact as? FFIContactDto) ?: (contact as? MergedContactDto)?.ffiContactDto

    fun getPhoneDto(): PhoneContactDto? = (contact as? PhoneContactDto) ?: (contact as? MergedContactDto)?.phoneContactDto

    fun getMergedDto(): MergedContactDto? = (contact as? MergedContactDto)
}