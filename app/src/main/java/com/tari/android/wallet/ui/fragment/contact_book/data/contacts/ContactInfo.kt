package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import kotlinx.parcelize.Parcelize

sealed class ContactInfo(
    open val firstName: String,
    open val lastName: String,
    open val isFavorite: Boolean,
    open val yatDto: YatDto? = null,
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

    // TODO I'm not sure we need this method
    override fun equals(other: Any?): Boolean {
        if (other is ContactInfo) {
            return firstName == other.firstName &&
                    lastName == other.lastName &&
                    isFavorite == other.isFavorite
        }
        return false
    }

    // TODO I'm not sure we need this method
    override fun hashCode(): Int = HashcodeUtils.generate(firstName, lastName, isFavorite)
}

@Parcelize
data class FFIContactInfo(
    val walletAddress: TariWalletAddress,
    @Transient override val firstName: String = "",
    @Transient override val lastName: String = "",
    @Transient override val isFavorite: Boolean = false,
    @Transient override val yatDto: YatDto? = null,
) : ContactInfo(firstName, lastName, isFavorite, yatDto) {

    constructor(walletAddress: TariWalletAddress, alias: String = "", isFavorite: Boolean = false) : this(
        walletAddress = walletAddress,
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
    val shouldUpdate: Boolean = false,
    val displayName: String,
    @Transient override val firstName: String = "",
    @Transient override val lastName: String = "",
    @Transient override val isFavorite: Boolean = false,
    @Transient override val yatDto: YatDto? = null,
) : ContactInfo(
    firstName = firstName.ifEmpty { parseAlias(displayName).first },
    lastName = lastName.ifEmpty { parseAlias(displayName).second },
    isFavorite = isFavorite,
    yatDto = yatDto,
) {
    override fun filtered(text: String): Boolean = getAlias().contains(text, ignoreCase = true)

    override fun extractWalletAddress(): TariWalletAddress = TariWalletAddress.createWalletAddress()

    fun extractDisplayName(): String = displayName.ifEmpty { "$firstName $lastName" }
}

@Parcelize
data class MergedContactInfo(
    val ffiContactInfo: FFIContactInfo,
    val phoneContactInfo: PhoneContactInfo,
    @Transient override val firstName: String = phoneContactInfo.firstName,
    @Transient override val lastName: String = phoneContactInfo.lastName,
    @Transient override val isFavorite: Boolean = phoneContactInfo.isFavorite,
    @Transient override val yatDto: YatDto? = ffiContactInfo.yatDto ?: phoneContactInfo.yatDto,
) : ContactInfo(firstName, lastName, isFavorite, yatDto) {
    override fun filtered(text: String): Boolean = ffiContactInfo.filtered(text) || phoneContactInfo.filtered(text)

    override fun extractWalletAddress(): TariWalletAddress = ffiContactInfo.walletAddress

    override fun getAlias(): String = phoneContactInfo.firstName
}

fun String.toYatDto(): YatDto? = this.takeIf { it.isNotEmpty() }?.let { YatDto(it) }

private fun parseAlias(alias: String): Pair<String, String> = alias.split(" ", limit = 2).let { it[0] to if (it.size > 1) it[1] else "" }