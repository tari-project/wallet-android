package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class PhoneContactDto(val id: String, var avatar: String, firstName: String = "", surname: String = "", isFavorite: Boolean = false) :
    IContact(firstName, surname, isFavorite) {

    override fun filtered(text: String): Boolean = getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress()
}