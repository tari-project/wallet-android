package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.adapter

import com.tari.android.wallet.application.addressPoisoning.SimilarAddressDto
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import java.util.Date

data class SimilarAddressItem(
    val dto: SimilarAddressDto,
    val lastItem: Boolean = false,
) : CommonViewHolderItem() {
    override val viewHolderUUID
        get() = dto.contact.walletAddress.fullBase58

    val contactName: String
        get() = dto.contact.alias.orEmpty()
    val numberOfTransaction: Int
        get() = dto.numberOfTransaction
    val lastTransactionDate: Date?
        get() = dto.lastTransactionTimestampMillis?.let { Date(it) }
    val trusted: Boolean
        get() = dto.trusted

    var selected: Boolean = false
}