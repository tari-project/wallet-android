package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.User
import com.tari.android.wallet.yat.YatUser
import java.io.Serializable

interface IContact : Serializable {
    fun filtered(text: String): Boolean

    companion object {

        fun generateFromUser(user: User): IContact = when (user) {
            is YatUser -> YatContactDto(user.walletAddress, user.yat, listOf(), user.walletAddress.emojiId)
            is Contact -> FFIContactDto(user.walletAddress, user.alias)
            else -> FFIContactDto(user.walletAddress, user.walletAddress.emojiId)
        }
    }

    fun extractWalletAddress(): TariWalletAddress

    fun getAlias(): String

    fun toUser(): User = when (val user = this) {
        is YatContactDto -> YatUser().apply {
            this.walletAddress = user.walletAddress
            this.yat = user.yat
        }

        is FFIContactDto -> Contact().apply {
            this.walletAddress = user.walletAddress
            this.alias = user.localAlias
        }

        is MergedContactDto -> Contact().apply {
            this.walletAddress = user.ffiContactDto.walletAddress
            this.alias = user.phoneContactDto.name
        }

        else -> throw IllegalArgumentException("Unknown contact type")
    }
}