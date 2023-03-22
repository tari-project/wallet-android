package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class PhoneContactDto(
    val id: String,
    var avatar: String,
    firstName: String = "",
    surname: String = "",
    isFavorite: Boolean = false
) : IContact(firstName, surname, isFavorite) {

    var displayName: String = ""
        get() = field.ifEmpty { "$firstName $surname" }
        set(value) {
            field = value
            if (firstName.isEmpty() && surname.isEmpty() && displayName.isNotEmpty()) {
                val name = value.split(" ")
                this.firstName = name.take(1).joinToString(" ")
                this.surname = name.drop(1).joinToString(" ")
            }
        }

    var shouldUpdate: Boolean = false

    override fun filtered(text: String): Boolean = getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress()
}