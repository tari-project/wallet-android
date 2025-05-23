package com.tari.android.wallet.data.contacts.model

import android.os.Parcelable
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.ContactAction
import com.tari.android.wallet.ffi.FFIContact
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import kotlinx.parcelize.Parcelize

sealed class ContactInfo(
    open val firstName: String,
    open val lastName: String,
    open val isFavorite: Boolean,
) : Parcelable {
    abstract fun filtered(text: String): Boolean
    abstract fun extractWalletAddress(): TariWalletAddress?
    abstract fun extractYat(): EmojiId?

    fun requireWalletAddress(): TariWalletAddress = extractWalletAddress()
        ?: error("Wallet address is required, but is null. Most probably this is a PhoneContactInfo which does not have a wallet address.")

    open fun getAlias(): String = "$firstName${if (firstName.isNotEmpty() && lastName.isNotEmpty()) " " else ""}$lastName"

    fun getContactActions(): List<ContactAction> = listOfNotNull(
        ContactAction.Send.takeIf { this is FFIContactInfo || this is MergedContactInfo },
        ContactAction.Link.takeIf { this is FFIContactInfo },
        ContactAction.Unlink.takeIf { this is MergedContactInfo },
        ContactAction.OpenProfile,
        ContactAction.EditName.takeIf { getAlias().isNotBlank() },
        ContactAction.AddContact.takeIf { getAlias().isBlank() },
        ContactAction.ToUnFavorite.takeIf { (this is FFIContactInfo || this is MergedContactInfo) && this.isFavorite },
        ContactAction.ToFavorite.takeIf { (this is FFIContactInfo || this is MergedContactInfo) && !this.isFavorite },
        ContactAction.Delete.takeIf { getAlias().isNotBlank() }, // the delete option is available only for added contacts (with alias)
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
        firstName = splitAlias(alias).firstName,
        lastName = splitAlias(alias).lastName,
        isFavorite = isFavorite,
    )

    constructor(tariContact: TariContact) : this(
        walletAddress = tariContact.walletAddress,
        firstName = splitAlias(tariContact.alias).firstName,
        lastName = splitAlias(tariContact.alias).lastName,
        isFavorite = tariContact.isFavorite,
    )

    constructor(ffiContact: FFIContact) : this(
        walletAddress = ffiContact.getWalletAddress().runWithDestroy { TariWalletAddress(it) },
        alias = ffiContact.getAlias(),
        isFavorite = ffiContact.getIsFavorite(),
    )

    override fun filtered(text: String): Boolean = walletAddress.fullEmojiId.contains(text, true) || getAlias().contains(text, true)

    override fun extractWalletAddress(): TariWalletAddress = walletAddress

    override fun extractYat(): EmojiId? = null

    override fun getAlias(): String = "$firstName $lastName".ifBlank { "" }
}

@Parcelize
data class PhoneContactInfo(
    val id: String,
    val avatar: String,
    val phoneEmojiId: EmojiId?,
    val phoneYat: EmojiId?,
    val shouldUpdate: Boolean = false,
    val displayName: String,
    override val firstName: String = "",
    override val lastName: String = "",
    override val isFavorite: Boolean = false,
) : ContactInfo(
    firstName = firstName.ifEmpty { splitAlias(displayName).firstName },
    lastName = lastName.ifEmpty { splitAlias(displayName).lastName },
    isFavorite = isFavorite,
) {
    override fun filtered(text: String): Boolean = getAlias().contains(text, ignoreCase = true)

    override fun extractWalletAddress(): TariWalletAddress? = null

    override fun extractYat(): EmojiId? = phoneYat
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

    override fun extractYat(): EmojiId? = phoneContactInfo.phoneYat

    override fun getAlias(): String = phoneContactInfo.getAlias()
}

fun splitAlias(alias: String): ParsedAlias = alias.split(" ", limit = 2)
    .let { ParsedAlias(firstName = it[0], lastName = if (it.size > 1) it[1] else "") }

data class ParsedAlias(val firstName: String, val lastName: String)