package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress

open class FFIContactDto() : IContact() {

    lateinit var walletAddress: TariWalletAddress

    constructor(walletAddress: TariWalletAddress, alias: String = "", isFavorite: Boolean = false) : this() {
        this.walletAddress = walletAddress
        this.isFavorite = isFavorite
        setAlias(alias)
    }

    constructor(walletAddress: TariWalletAddress, firstName: String, surname: String, isFavorite: Boolean) : this() {
        this.walletAddress = walletAddress
        this.firstName = firstName
        this.surname = surname
        this.isFavorite = isFavorite
    }

    constructor(tariContact: TariContact) : this() {
        this.walletAddress = tariContact.walletAddress
        setAlias(tariContact.alias)
    }

    fun setAlias(alias: String) {
        val (firstName, secondName) = alias.split(" ").toMutableList().apply { if (size == 1) this.add("") }
        this.firstName = firstName
        this.surname = secondName
    }

    override fun filtered(text: String): Boolean = walletAddress.emojiId.contains(text, true) || getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = walletAddress

    override fun getAlias(): String = "$firstName $surname".ifBlank { "" }
}