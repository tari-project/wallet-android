package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.model.TariWalletAddress

class MergedContactDto(var ffiContactDto: FFIContactDto, var phoneContactDto: PhoneContactDto) : IContact {

    override fun filtered(text: String): Boolean = ffiContactDto.filtered(text) || phoneContactDto.filtered(text)

    override fun extractWalletAddress(): TariWalletAddress = ffiContactDto.walletAddress

    override fun getAlias(): String = phoneContactDto.name
}