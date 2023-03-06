package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.TariWalletAddress

open class FFIContactDto() : IContact {

    lateinit var walletAddress: TariWalletAddress

    var localAlias: String = ""

    constructor(walletAddress: TariWalletAddress, alias: String = "") : this() {
        this.walletAddress = walletAddress
        this.localAlias = alias
    }

    constructor(contact: Contact) : this() {
        this.walletAddress = contact.walletAddress
        this.localAlias = contact.alias
    }

    override fun filtered(text: String): Boolean = walletAddress.emojiId.contains(text, true) || localAlias.contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = walletAddress

    override fun getAlias(): String = localAlias
}