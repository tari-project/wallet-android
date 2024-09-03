package com.tari.android.wallet.ui.fragment.contactBook.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactAction
import com.tari.android.wallet.util.EmojiId
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ContactDto(
    val contactInfo: ContactInfo,
    val uuid: String = UUID.randomUUID().toString(),
    val yatDto: YatDto? = null,
) : Parcelable {
    constructor(contactInfo: PhoneContactInfo) : this(contactInfo, yatDto = contactInfo.phoneYat.toYatDto())

    val walletAddress: TariWalletAddress?
        get() = contactInfo.extractWalletAddress()

    fun filtered(text: String): Boolean = contactInfo.filtered(text)

    fun getContactActions(): List<ContactAction> = contactInfo.getContactActions()

    fun getTypeName(): Int = contactInfo.getTypeName()

    fun getTypeIcon(): Int = contactInfo.getTypeIcon()

    fun getFFIContactInfo(): FFIContactInfo? = (contactInfo as? FFIContactInfo) ?: (contactInfo as? MergedContactInfo)?.ffiContactInfo

    fun getPhoneContactInfo(): PhoneContactInfo? = (contactInfo as? PhoneContactInfo) ?: (contactInfo as? MergedContactInfo)?.phoneContactInfo
}

fun EmojiId.toYatDto(): YatDto? = this.takeIf { it.isNotEmpty() }?.let { YatDto(it) }