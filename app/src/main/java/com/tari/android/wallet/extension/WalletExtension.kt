package com.tari.android.wallet.extension

import com.orhanobut.logger.Logger
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.WalletError.Companion.NoError
import com.tari.android.wallet.model.WalletException
import com.tari.android.wallet.model.throwIf
import com.tari.android.wallet.service.TariWalletService

fun TariWalletService.executeWithError(
    onError: (error: WalletError) -> Unit = { throwIf(it) },
    action: (error: WalletError, wallet: TariWalletService) -> Unit
) {
    val walletError = WalletError()
    action(walletError, this)
    onError(walletError)
}

fun <T> TariWalletService.getWithError(
    onError: (error: WalletError) -> Unit = { throwIf(it) },
    action: (error: WalletError, wallet: TariWalletService) -> T,
): T {
    val walletError = WalletError()
    val result = action(walletError, this)
    onError(walletError)
    return result
}

fun <T> TariWalletService.getResultWithError(
    action: (error: WalletError, service: TariWalletService) -> T,
): Result<T> {
    val walletError = WalletError()
    val result = action(walletError, this)

    if (walletError.code != NoError.code) {
        Logger.t("WalletService").i("Error while wallet operation: ${walletError.signature}")
        return Result.failure(WalletException(walletError))
    }
    return Result.success(result)
}