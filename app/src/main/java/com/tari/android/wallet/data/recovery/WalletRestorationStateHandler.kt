package com.tari.android.wallet.data.recovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRestorationStateHandler @Inject constructor() {

    private val _walletRestorationState = MutableStateFlow<WalletRestorationState>(WalletRestorationState.NotStarted)
    val walletRestorationState = _walletRestorationState.asStateFlow()

    fun updateState(newState: WalletRestorationState) {
        _walletRestorationState.update { newState }
    }

    fun isWalletRestoring(): Boolean = _walletRestorationState.value !is WalletRestorationState.NotStarted &&
            _walletRestorationState.value !is WalletRestorationState.Completed

    fun clear() {
        _walletRestorationState.value = WalletRestorationState.NotStarted
    }
}