package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.model.User
import java.io.Serializable
import java.util.UUID

class ContactDto(val user: User, var isFavorite: Boolean, var uuid: String = UUID.randomUUID().toString()) : Serializable {
    fun filtered(text: String): Boolean = user.filtered(text)
}