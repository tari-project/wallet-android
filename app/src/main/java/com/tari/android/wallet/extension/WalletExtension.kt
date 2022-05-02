package com.tari.android.wallet.extension

import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.throwIf
import com.tari.android.wallet.service.TariWalletService

internal fun TariWalletService.executeWithError(
    onError: (error: WalletError) -> Unit = { throwIf(it) },
    action: (error: WalletError, wallet: TariWalletService) -> Unit
) {
    val walletError = WalletError()
    action(walletError, this)
    onError(walletError)
}

internal fun <T> TariWalletService.getWithError(
    onError: (error: WalletError) -> Unit = { throwIf(it) },
    action: (error: WalletError, wallet: TariWalletService) -> T,
): T {
    val walletError = WalletError()
    val result = action(walletError, this)
    onError(walletError)
    return result
}