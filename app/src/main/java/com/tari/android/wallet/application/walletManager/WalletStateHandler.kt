package com.tari.android.wallet.application.walletManager

import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.FFIWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

private val logger
    get() = Logger.t(WalletManager::class.simpleName)

suspend fun WalletManager.doOnWalletStarted(action: suspend (ffiWallet: FFIWallet) -> Unit) = withContext(Dispatchers.IO) {
    walletState.firstOrNull { it is WalletState.Started }
        ?.let {
            action(walletInstance!!)
        } ?: logger.i("Wallet service is not connected")
}

suspend fun WalletManager.doOnWalletRunning(action: suspend (ffiWallet: FFIWallet) -> Unit) = withContext(Dispatchers.IO) {
    walletState.firstOrNull { it is WalletState.Running }
        ?.let {
            action(walletInstance!!)
        } ?: logger.i("Wallet service is not connected")
}

suspend fun <T> WalletManager.doOnWalletRunningWithValue(action: suspend (ffiWallet: FFIWallet) -> T): T = withContext(Dispatchers.IO) {
    walletState.firstOrNull { it is WalletState.Running }
        ?.let {
            action(walletInstance!!)
        } ?: error("Wallet service is not connected")
}

suspend fun WalletManager.doOnWalletFailed(action: suspend (exception: Exception) -> Unit) = withContext(Dispatchers.IO) {
    walletState
        .debounce(300L) // todo this is a workaround for the issue that the wallet service is not connected yet
        .firstOrNull { it is WalletState.Failed }
        ?.let {
            action((it as WalletState.Failed).exception)
        } ?: logger.i("Wallet service is not connected")
}

suspend fun WalletManager.doOnWalletNotReady(action: suspend () -> Unit) = withContext(Dispatchers.IO) {
    walletState.firstOrNull { it is WalletState.NotReady }
        ?.let {
            action()
        } ?: logger.i("Wallet service is not connected")
}
