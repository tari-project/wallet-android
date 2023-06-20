package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.util.extractEmojis
import java.io.Serializable
import java.util.UUID

data class ContactDto(
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
        is FFIContactDto -> R.string.contact_book_type_ffi
        is MergedContactDto -> R.string.contact_book_type_merged
        else -> R.string.contact_book_type_contact_book
    }

    fun getTypeIcon(): Int = when (contact) {
        is FFIContactDto -> R.drawable.vector_gem
        is MergedContactDto -> R.drawable.vector_contact_type_link
        else -> R.drawable.vector_contact_book_type
    }

    fun getYatDto(): YatDto? = (contact as? PhoneContactDto)?.yatDto ?: (contact as? MergedContactDto)?.phoneContactDto?.yatDto

    fun getFFIDto(): FFIContactDto? = (contact as? FFIContactDto) ?: (contact as? MergedContactDto)?.ffiContactDto

    fun getPhoneDto(): PhoneContactDto? = (contact as? PhoneContactDto) ?: (contact as? MergedContactDto)?.phoneContactDto

    fun getMergedDto(): MergedContactDto? = (contact as? MergedContactDto)

    companion object {
        fun getDefaultAlias(walletAddress: TariWalletAddress): String =
            "Aurora User " + walletAddress.emojiId.extractEmojis().take(3).joinToString("")

        fun normalizeAlias(alias: String?, walletAddress: TariWalletAddress): String {
            return alias.orEmpty().ifBlank { getDefaultAlias(walletAddress) }
        }
    }

    override fun hashCode(): Int = HashcodeUtils.generate(contact, uuid, lastUsedDate)
}