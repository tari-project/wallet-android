package com.tari.android.wallet.extension

import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.throwIf
import com.tari.android.wallet.service.TariWalletService

internal fun TariWalletService.executeWithError(action: (error: WalletError, wallet: TariWalletService) -> Unit) {
    val walletError = WalletError()
    action(walletError, this)
    throwIf(walletError)
}

internal fun <T> TariWalletService.getWithError(action: (error: WalletError, wallet: TariWalletService) -> T): T {
    val walletError = WalletError()
    val result = action(walletError, this)
    throwIf(walletError)
    return result
}