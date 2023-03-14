package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import java.io.Serializable
import java.util.Random
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

    fun getYatDto(): YatContactDto? = (contact as? YatContactDto) ?: (contact as? MergedContactDto)?.ffiContactDto as? YatContactDto

    fun getFFIDto(): FFIContactDto? = (contact as? FFIContactDto) ?: (contact as? MergedContactDto)?.ffiContactDto

    fun getPhoneDto(): PhoneContactDto? = (contact as? PhoneContactDto) ?: (contact as? MergedContactDto)?.phoneContactDto
}