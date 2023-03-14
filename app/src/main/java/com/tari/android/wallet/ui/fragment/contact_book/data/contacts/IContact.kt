package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress
import java.io.Serializable

abstract class IContact : Serializable {

    var firstName: String = ""
    var surname: String = ""
    var isFavorite: Boolean = false

    constructor(firstName: String = "", surname: String = "", isFavorite: Boolean = false) {
        this.firstName = firstName
        this.surname = surname
        this.isFavorite = isFavorite
    }

    abstract fun filtered(text: String): Boolean

    abstract fun extractWalletAddress(): TariWalletAddress

    open fun getAlias(): String = "$firstName $surname"
}