package com.tari.android.wallet.ui.fragment.contact_book.data.contacts

import com.google.gson.annotations.SerializedName
import com.tari.android.wallet.R
import yat.android.data.YatRecordType
import yat.android.sdk.models.PaymentAddressResponseResult
import java.io.Serializable
import java.lang.reflect.Field

class YatDto(var yat: String, var connectedWallets: List<ConnectedWallet> = listOf()) {

    fun filtered(text: String): Boolean = yat.contains(text, true)

    class ConnectedWallet(val key: String, val value: PaymentAddressResponseResult) : Serializable {

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
            val names = YatRecordType.values().associateBy { getSerializedName(it.javaClass.getField(it.name)) }

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