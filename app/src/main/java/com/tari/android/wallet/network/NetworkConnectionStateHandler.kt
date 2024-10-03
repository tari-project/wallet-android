package com.tari.android.wallet.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectionStateHandler @Inject constructor() {

    private val _networkConnectionState = MutableStateFlow(NetworkConnectionState.UNKNOWN)
    val networkConnectionState = _networkConnectionState.asStateFlow()

    fun updateState(state: NetworkConnectionState) {
        _networkConnectionState.value = state
    }

    fun isNetworkConnected(): Boolean {
        return networkConnectionState.value == NetworkConnectionState.CONNECTED
    }
}