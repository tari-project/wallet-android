package com.tari.android.wallet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CompletedTransactionKernel(
    val excess: String,
    val publicNonce: String,
    val signature: String,
) : Parcelable