package com.tari.android.wallet.application.walletManager

import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.FFIWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletStateHandler @Inject constructor() {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val _walletState = MutableStateFlow<WalletState>(WalletState.NotReady)
    val walletState = _walletState.asStateFlow()

    fun setWalletState(state: WalletState) {
        _walletState.update { state }
    }

    suspend fun doOnWalletStarted(action: suspend (ffiWallet: FFIWallet) -> Unit) = withContext(Dispatchers.IO) {
        walletState.firstOrNull { it is WalletState.Started }
            ?.let {
                action(FFIWallet.instance!!)
            } ?: logger.i("Wallet service is not connected")
    }

    suspend fun doOnWalletRunning(action: suspend (ffiWallet: FFIWallet) -> Unit) = withContext(Dispatchers.IO) {
        walletState.firstOrNull { it is WalletState.Running }
            ?.let {
                action(FFIWallet.instance!!)
            } ?: logger.i("Wallet service is not connected")
    }

    suspend fun <T> doOnWalletRunningWithValue(action: suspend (ffiWallet: FFIWallet) -> T): T = withContext(Dispatchers.IO) {
        walletState.firstOrNull { it is WalletState.Running }
            ?.let {
                action(FFIWallet.instance!!)
            } ?: error("Wallet service is not connected")
    }

    suspend fun doOnWalletFailed(action: suspend (exception: Exception) -> Unit) = withContext(Dispatchers.IO) {
        walletState
            .debounce(300L) // todo this is a workaround for the issue that the wallet service is not connected yet
            .firstOrNull { it is WalletState.Failed }
            ?.let {
                action((it as WalletState.Failed).exception)
            } ?: logger.i("Wallet service is not connected")
    }

    suspend fun doOnWalletNotReady(action: suspend () -> Unit) = withContext(Dispatchers.IO) {
        walletState.firstOrNull { it is WalletState.NotReady }
            ?.let {
                action()
            } ?: logger.i("Wallet service is not connected")
    }
}
