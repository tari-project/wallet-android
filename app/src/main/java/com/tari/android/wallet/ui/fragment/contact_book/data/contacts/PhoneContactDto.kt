package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class PhoneContactDto(val id: String, var firstName: String, var surname: String, var avatar: String) : IContact {
    override fun filtered(text: String): Boolean = getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress()

    override fun getAlias(): String = "$firstName $surname"
}