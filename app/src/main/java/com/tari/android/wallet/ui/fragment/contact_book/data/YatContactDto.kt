package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.model.TariWalletAddress
import java.io.Serializable

class YatContactDto(walletAddress: TariWalletAddress, var yat: String, val connectedWallets: List<ConnectedWallet>, alias: String = "") : FFIContactDto(walletAddress, alias) {
    override fun filtered(text: String): Boolean = super.filtered(text) || yat.contains(text, true)

    override fun getAlias(): String = localAlias

    class ConnectedWallet(val name: String) : Serializable
}