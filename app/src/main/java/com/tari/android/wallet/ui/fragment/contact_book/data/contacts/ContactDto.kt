package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ContactDto(
    val contactInfo: ContactInfo,
    val uuid: String = UUID.randomUUID().toString(),
    var lastUsedDate: SerializableTime? = null,
    val yat: YatDto? = contactInfo.yatDto // TODO could be not in contactInfo ?
) : Parcelable {
    val walletAddress: TariWalletAddress
        get() = contactInfo.extractWalletAddress()

    fun filtered(text: String): Boolean = contactInfo.filtered(text)

    fun getContactActions(): List<ContactAction> = contactInfo.getContactActions()

    fun getTypeName(): Int = contactInfo.getTypeName()

    fun getTypeIcon(): Int = contactInfo.getTypeIcon()

    fun getYatDto() = yat

    fun getFFIContactInfo(): FFIContactInfo? = (contactInfo as? FFIContactInfo) ?: (contactInfo as? MergedContactInfo)?.ffiContactInfo

    fun getPhoneContactInfo(): PhoneContactInfo? = (contactInfo as? PhoneContactInfo) ?: (contactInfo as? MergedContactInfo)?.phoneContactInfo

    // TODO I'm not sure we need this method
    override fun equals(other: Any?): Boolean {
        if (other is ContactDto) {
            return contactInfo == other.contactInfo &&
                    uuid == other.uuid &&
                    lastUsedDate == other.lastUsedDate
        }
        return false
    }

    // TODO I'm not sure we need this method
    override fun hashCode(): Int = HashcodeUtils.generate(contactInfo, uuid, lastUsedDate)
}