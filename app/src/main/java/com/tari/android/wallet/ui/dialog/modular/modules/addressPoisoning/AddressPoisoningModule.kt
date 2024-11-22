package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning

import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.adapter.SimilarAddressItem
import com.tari.android.wallet.ui.screen.contactBook.addressPoisoning.SimilarAddressDto

data class AddressPoisoningModule(
    private val addresses: List<SimilarAddressDto> = emptyList(),
) : IDialogModule() {
    val addressItems: List<SimilarAddressItem> = addresses
        .mapIndexed { index, addressDto -> SimilarAddressItem(dto = addressDto, lastItem = index == addresses.lastIndex) }
        .also { it.first().selected = true }

    val selectedAddressItem: SimilarAddressItem
        get() = addressItems.first { it.selected }

    val selectedAddress: SimilarAddressDto
        get() = selectedAddressItem.dto

    var markAsTrusted: Boolean = false
}