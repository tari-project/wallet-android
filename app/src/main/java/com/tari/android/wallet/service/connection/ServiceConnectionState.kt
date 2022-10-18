package com.tari.android.wallet.service.connection

import com.tari.android.wallet.service.TariWalletService

data class ServiceConnectionState(
    val status: ServiceConnectionStatus,
    val service: TariWalletService?
)