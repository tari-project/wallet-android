package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.google.api.client.util.DateTime
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import java.io.Serializable
import java.util.Random
import java.util.UUID

class ContactDto(val contact: IContact, var isFavorite: Boolean = false, var uuid: String = UUID.randomUUID().toString(), var lastUsedDate: SerializableTime? = null, var isDeleted: Boolean = false) : Serializable {
    fun filtered(text: String): Boolean = contact.filtered(text)

    companion object {
        fun generate() : IContact {
            val pos = Random().nextInt(4)
            val walletAddress = TariWalletAddress().apply {
                emojiId = "\uD83C\uDFA4\uD83D\uDCCC\uD83D\uDD11\uD83D\uDC22\uD83D\uDEA6\uD83C\uDF4A\uD83D\uDC36\uD83C\uDFE0\uD83C\uDF1E\uD83C\uDF92\uD83C\uDFBD\uD83D\uDC1A\uD83D\uDC2D\uD83C\uDFE0\uD83C\uDF46\uD83C\uDFB9\uD83C\uDF41\uD83D\uDC5E\uD83C\uDF02\uD83C\uDF82\uD83D\uDCF1\uD83C\uDF76\uD83D\uDE0D\uD83C\uDFAC\uD83D\uDE37\uD83D\uDC90\uD83D\uDC18\uD83D\uDD0C\uD83D\uDEA8\uD83C\uDFE0\uD83C\uDF45\uD83C\uDF82\uD83D\uDD26"
            }
            val alias = ""
            val name = "Name"
            val yat = "Yat"
            val connectedWallets = (0..Random().nextInt(10)).map { YatContactDto.ConnectedWallet("Wallet $it") }.toList()
            return when(pos) {
                0 -> FFIContactDto(walletAddress, alias)
                1 -> PhoneContactDto(name)
                2 -> MergedContactDto(FFIContactDto(walletAddress, alias), PhoneContactDto(name))
                else -> YatContactDto(walletAddress, yat, connectedWallets, alias)
            }
        }

        fun generateContactDto() : ContactDto = ContactDto(generate(), Random().nextBoolean())
    }

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

        if (isFavorite) {
            actions.add(ContactAction.ToUnFavorite)
        } else {
            actions.add(ContactAction.ToFavorite)
        }

        actions.add(ContactAction.Delete)

        return actions
    }
}