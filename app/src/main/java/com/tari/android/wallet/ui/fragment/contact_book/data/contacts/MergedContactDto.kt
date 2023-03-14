package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress

class MergedContactDto(var ffiContactDto: FFIContactDto, var phoneContactDto: PhoneContactDto) : IContact() {

    init {
        firstName = phoneContactDto.firstName
        surname = phoneContactDto.surname
        isFavorite = phoneContactDto.isFavorite || ffiContactDto.isFavorite
    }

    override fun filtered(text: String): Boolean = ffiContactDto.filtered(text) || phoneContactDto.filtered(text)

    override fun extractWalletAddress(): TariWalletAddress = ffiContactDto.walletAddress

    override fun getAlias(): String = phoneContactDto.firstName
}