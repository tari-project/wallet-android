package com.tari.android.wallet.ui.dialog.error

import com.tari.android.wallet.R
import com.tari.android.wallet.model.CoreError
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs

class WalletErrorArgs(
    val resourceManager: ResourceManager,
    val error: CoreError,
    val dismissAction: () -> Unit = {},
) {
    constructor(resourceManager: ResourceManager, exception: Throwable, dismissAction: () -> Unit = { }) : this(
        resourceManager = resourceManager,
        error = WalletError(exception),
        dismissAction = dismissAction,
    )

    val title: String
        get() = error.signature

    val description: String
        get() {
            val id = when (error) {
                WalletError.DatabaseDataError -> R.string.error_wallet_message_114
                WalletError.TransactionNotFoundError -> R.string.error_wallet_message_204
                WalletError.ContactNotFoundError -> R.string.error_wallet_message_401
                WalletError.InvalidPassphraseEncryptionCypherError -> R.string.error_wallet_message_420
                WalletError.InvalidPassphraseError -> R.string.error_wallet_message_428
                WalletError.SeedWordsInvalidDataError -> R.string.error_wallet_message_429
                WalletError.SeedWordsVersionMismatchError -> R.string.error_wallet_message_430
                WalletError.RecoveringWrongAddress -> R.string.error_wallet_message_702
                else -> R.string.error_wallet_message_generic
            }
            return resourceManager.getString(id)
        }

    fun getModular() = SimpleDialogArgs(
        title = title,
        description = description,
        onClose = dismissAction,
    ).getModular(resourceManager)
}