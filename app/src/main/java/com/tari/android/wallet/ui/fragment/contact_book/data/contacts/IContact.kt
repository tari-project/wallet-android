package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.tari.android.wallet.model.TariWalletAddress
import java.io.Serializable

abstract class IContact(open var firstName: String = "", open var surname: String = "", open var isFavorite: Boolean = false) : Serializable {

    abstract fun filtered(text: String): Boolean

    abstract fun extractWalletAddress(): TariWalletAddress

    open fun getAlias(): String = "$firstName $surname"
}