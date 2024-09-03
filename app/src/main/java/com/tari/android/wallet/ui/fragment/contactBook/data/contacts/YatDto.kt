package com.tari.android.wallet.ui.fragment.contactBook.data.contacts

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto.ConnectedWallet
import com.tari.android.wallet.util.EmojiId
import kotlinx.parcelize.Parcelize
import yat.android.data.YatRecordType
import yat.android.sdk.models.PaymentAddressResponseResult
import java.lang.reflect.Field

@Parcelize
data class YatDto(
    val yat: EmojiId,
    val connectedWallets: List<ConnectedWallet> = listOf(), // TODO don't store connected wallets in this class. They should be loaded on the Contact Detail screen
) : Parcelable {

    @Parcelize
    data class ConnectedWallet(
        val key: String,
        val value: PaymentAddressResponseResult,
    ) : Parcelable {

        private val yatRecordType: YatRecordType?
            get() = names[key]

        val name: Int?
            get() {
                return when (yatRecordType) {
                    YatRecordType.BTC_ADDRESS -> R.string.contact_book_details_connected_wallets_bitcoin
                    YatRecordType.ETH_ADDRESS -> R.string.contact_book_details_connected_wallets_etherium
                    YatRecordType.XMR_STANDARD_ADDRESS -> R.string.contact_book_details_connected_wallets_monero
                    else -> null
                }
            }

        fun getExternalLink(): String = when (yatRecordType) {
            YatRecordType.BTC_ADDRESS -> "bitcoin:${value.address}"
            YatRecordType.ETH_ADDRESS -> {
                if (!value.address.startsWith("0x")) {
                    "ethereum:pay-0x${value.address}"
                } else {
                    "ethereum:pay-${value.address}"
                }
            }

            YatRecordType.XMR_STANDARD_ADDRESS -> "monero:${value.address}"
            else -> value.address
        }

        companion object {
            val names = YatRecordType.entries.associateBy { getSerializedName(it.javaClass.getField(it.name)) }

            private fun getSerializedName(enumField: Field): String? {
                if (enumField.isAnnotationPresent(SerializedName::class.java)) {
                    val fieldEnrich = enumField.getAnnotation(SerializedName::class.java)
                    return fieldEnrich?.value
                }
                return ""
            }
        }
    }
}

fun YatDto?.withConnectedWallets(connectedWalletsMap: Map<String, PaymentAddressResponseResult>): YatDto? =
    this?.copy(connectedWallets = connectedWalletsMap.toConnectedWallets())

fun Map<String, PaymentAddressResponseResult>.toConnectedWallets(): List<ConnectedWallet> = map { ConnectedWallet(it.key, it.value) }