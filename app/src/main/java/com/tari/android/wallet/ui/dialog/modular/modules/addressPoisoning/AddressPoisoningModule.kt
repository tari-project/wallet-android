package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning

import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.fragment.contact_book.address_poisoning.SimilarAddressItem

data class AddressPoisoningModule(
    val addresses: List<SimilarAddressItem> = emptyList(),
    var markAsTrusted: Boolean = false,
) : IDialogModule() {
    val selectedAddress: SimilarAddressItem?
        get() = addresses.firstOrNull { it.selected }
}