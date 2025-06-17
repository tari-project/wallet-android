package com.tari.android.wallet.data.contacts

import android.os.Parcelable
import com.tari.android.wallet.model.TariWalletAddress
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val walletAddress: TariWalletAddress,
    val alias: String? = null,
) : Parcelable