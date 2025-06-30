package com.tari.android.wallet.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.extension.isTrue
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val walletAddress: TariWalletAddress,
    val alias: String? = null, // TODO think about making this non-nullable
) : Parcelable {

    fun contains(query: String): Boolean {
        return alias?.contains(query, ignoreCase = true).isTrue() ||
                walletAddress.contains(query)
    }
}