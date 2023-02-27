package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.model.TariWalletAddress

class PhoneContactDto(var name: String) : IContact {
    override fun filtered(text: String): Boolean = name.contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = throw UnsupportedOperationException()

    override fun getAlias(): String = name

    override fun getContactActions(): List<ContactAction> = super.getContactActions().filter { it != ContactAction.Send }
}