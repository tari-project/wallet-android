package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class PhoneContactDto(var name: String) : IContact {
    override fun filtered(text: String): Boolean = name.contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress()

    override fun getAlias(): String = name
}