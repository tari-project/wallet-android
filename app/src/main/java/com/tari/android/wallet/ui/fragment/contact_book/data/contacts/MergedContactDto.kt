package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class MergedContactDto(var ffiContactDto: FFIContactDto, var phoneContactDto: PhoneContactDto) : IContact() {

    override var firstName: String
        get() = phoneContactDto.firstName
        set(value) {
            phoneContactDto.firstName = value
        }

    override var surname: String
        get() = phoneContactDto.surname
        set(value) {
            phoneContactDto.surname = value
        }

    override var isFavorite: Boolean
        get() = phoneContactDto.isFavorite
        set(value) {
            phoneContactDto.isFavorite = value
        }

    override fun filtered(text: String): Boolean = ffiContactDto.filtered(text) || phoneContactDto.filtered(text)

    override fun extractWalletAddress(): TariWalletAddress = ffiContactDto.walletAddress

    override fun getAlias(): String = phoneContactDto.firstName
}