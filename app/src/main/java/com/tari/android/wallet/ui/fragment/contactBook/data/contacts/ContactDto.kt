package com.tari.android.wallet.ui.fragment.contactBook.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactAction
import com.tari.android.wallet.model.EmojiId
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ContactDto(
    val contactInfo: ContactInfo,
    val uuid: String = UUID.randomUUID().toString(),
) : Parcelable {
    val walletAddress: TariWalletAddress?
        get() = contactInfo.extractWalletAddress()

    val yat: EmojiId?
        get() = contactInfo.extractYat().takeIf { !it.isNullOrBlank() }

    val alias: String
        get() = contactInfo.getAlias()

    fun filtered(text: String): Boolean = contactInfo.filtered(text)

    fun getContactActions(): List<ContactAction> = contactInfo.getContactActions()

    fun getTypeName(): Int = contactInfo.getTypeName()

    fun getTypeIcon(): Int = contactInfo.getTypeIcon()

    fun getFFIContactInfo(): FFIContactInfo? = (contactInfo as? FFIContactInfo) ?: (contactInfo as? MergedContactInfo)?.ffiContactInfo

    fun getPhoneContactInfo(): PhoneContactInfo? = (contactInfo as? PhoneContactInfo) ?: (contactInfo as? MergedContactInfo)?.phoneContactInfo
}
