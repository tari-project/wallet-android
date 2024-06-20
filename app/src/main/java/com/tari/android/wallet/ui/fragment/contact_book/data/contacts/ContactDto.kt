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
    val yatDto: YatDto? = null,
) : Parcelable {
    val walletAddress: TariWalletAddress
        get() = contactInfo.extractWalletAddress()

    fun filtered(text: String): Boolean = contactInfo.filtered(text)

    fun getContactActions(): List<ContactAction> = contactInfo.getContactActions()

    fun getTypeName(): Int = contactInfo.getTypeName()

    fun getTypeIcon(): Int = contactInfo.getTypeIcon()

    fun getFFIContactInfo(): FFIContactInfo? = (contactInfo as? FFIContactInfo) ?: (contactInfo as? MergedContactInfo)?.ffiContactInfo

    fun getPhoneContactInfo(): PhoneContactInfo? = (contactInfo as? PhoneContactInfo) ?: (contactInfo as? MergedContactInfo)?.phoneContactInfo

    override fun equals(other: Any?): Boolean {
        if (other is ContactDto) {
            return contactInfo == other.contactInfo &&
                    uuid == other.uuid &&
                    lastUsedDate == other.lastUsedDate
        }
        return false
    }

    override fun hashCode(): Int = HashcodeUtils.generate(contactInfo, uuid, lastUsedDate)
}

fun String.toYatDto(): YatDto? = this.takeIf { it.isNotEmpty() }?.let { YatDto(it) }