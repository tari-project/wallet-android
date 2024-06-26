package com.tari.android.wallet.ui.fragment.contactBook.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactAction
import kotlinx.parcelize.Parcelize

sealed class ContactInfo(
    open val firstName: String,
    open val lastName: String,
    open val isFavorite: Boolean,
) : Parcelable {
    abstract fun filtered(text: String): Boolean
    abstract fun extractWalletAddress(): TariWalletAddress

    open fun getAlias(): String = "$firstName $lastName"

    fun getContactActions(): List<ContactAction> = listOfNotNull(
        ContactAction.Send.takeIf { this is FFIContactInfo || this is MergedContactInfo },
        ContactAction.Link.takeIf { this is FFIContactInfo },
        ContactAction.Unlink.takeIf { this is MergedContactInfo },
        ContactAction.OpenProfile,
        ContactAction.EditName,
        ContactAction.ToUnFavorite.takeIf { this.isFavorite },
        ContactAction.ToFavorite.takeIf { !this.isFavorite },
        ContactAction.Delete,
    )

    fun getTypeName(): Int = when (this) {
        is FFIContactInfo -> R.string.contact_book_type_ffi
        is MergedContactInfo -> R.string.contact_book_type_merged
        is PhoneContactInfo -> R.string.contact_book_type_contact_book
    }

    fun getTypeIcon(): Int = when (this) {
        is FFIContactInfo -> R.drawable.vector_gem
        is MergedContactInfo -> R.drawable.vector_contact_type_link
        is PhoneContactInfo -> R.drawable.vector_contact_book_type
    }
}

@Parcelize
data class FFIContactInfo(
    val walletAddress: TariWalletAddress,
    val lastUsedTimeMillis: Long = 0L,
    override val firstName: String = "",
    override val lastName: String = "",
    override val isFavorite: Boolean = false,
) : ContactInfo(firstName, lastName, isFavorite) {

    constructor(walletAddress: TariWalletAddress, lastUsedTimeMillis: Long = 0L, alias: String = "", isFavorite: Boolean = false) : this(
        walletAddress = walletAddress,
        lastUsedTimeMillis = lastUsedTimeMillis,
        firstName = parseAlias(alias).first,
        lastName = parseAlias(alias).second,
        isFavorite = isFavorite,
    )

    constructor(tariContact: TariContact) : this(
        walletAddress = tariContact.walletAddress,
        firstName = parseAlias(tariContact.alias).first,
        lastName = parseAlias(tariContact.alias).second,
        isFavorite = tariContact.isFavorite,
    )

    override fun filtered(text: String): Boolean = walletAddress.emojiId.contains(text, true) || getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = walletAddress

    override fun getAlias(): String = "$firstName $lastName".ifBlank { "" }
}

@Parcelize
data class PhoneContactInfo(
    val id: String,
    val avatar: String,
    val phoneEmojiId: String = "",
    val phoneYat: String = "",
    val shouldUpdate: Boolean = false,
    val displayName: String,
    override val firstName: String = "",
    override val lastName: String = "",
    override val isFavorite: Boolean = false,
) : ContactInfo(
    firstName = firstName.ifEmpty { parseAlias(displayName).first },
    lastName = lastName.ifEmpty { parseAlias(displayName).second },
    isFavorite = isFavorite,
) {
    override fun filtered(text: String): Boolean = getAlias().contains(text, ignoreCase = true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress.createWalletAddress()

    fun extractDisplayName(): String = displayName.ifEmpty { "$firstName $lastName" }
}

@Parcelize
data class MergedContactInfo(
    val ffiContactInfo: FFIContactInfo,
    val phoneContactInfo: PhoneContactInfo,
    override val firstName: String = phoneContactInfo.firstName,
    override val lastName: String = phoneContactInfo.lastName,
    override val isFavorite: Boolean = phoneContactInfo.isFavorite,
) : ContactInfo(firstName, lastName, isFavorite) {
    override fun filtered(text: String): Boolean = ffiContactInfo.filtered(text) || phoneContactInfo.filtered(text)

    override fun extractWalletAddress(): TariWalletAddress = ffiContactInfo.walletAddress

    override fun getAlias(): String = phoneContactInfo.firstName
}

private fun parseAlias(alias: String): Pair<String, String> = alias.split(" ", limit = 2).let { it[0] to if (it.size > 1) it[1] else "" }