package com.tari.android.wallet.util

import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.domain.ResourceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactUtil @Inject constructor(
    private val resourceManager: ResourceManager,
) {
    fun normalizeAlias(alias: String?, walletAddress: TariWalletAddress): String {
        return alias.orEmpty().ifBlank { getDefaultAlias(walletAddress) }
    }

    private fun getDefaultAlias(walletAddress: TariWalletAddress): String =
        resourceManager.getString(R.string.contact_book_default_alias, walletAddress.emojiId.extractEmojis().take(3).joinToString(""))
}