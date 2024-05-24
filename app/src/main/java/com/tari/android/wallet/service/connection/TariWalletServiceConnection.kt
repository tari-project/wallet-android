/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.service.connection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.service.WalletService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariWalletServiceConnection @Inject constructor(
    private val application: TariWalletApplication,
) : ViewModel(), ServiceConnection {
    private val logger
        get() = Logger.t(this::class.simpleName)

    private val _connection = MutableStateFlow<ServiceConnectionState>(ServiceConnectionState.NotYetConnected)
    private val connectionState = _connection.asStateFlow()
    val walletService
        get() = connectionState.value.safeCastTo<ServiceConnectionState.Connected>()?.service
            ?: error("Accessing wallet service before it is connected")

    init {
        reconnectToService()
    }

    private fun reconnectToService() {
        _connection.update { ServiceConnectionState.NotYetConnected }
        val bindIntent = Intent(application, WalletService::class.java)
        application.bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        _connection.update { ServiceConnectionState.Disconnected }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        _connection.update { ServiceConnectionState.Connected(TariWalletService.Stub.asInterface(service)) }
    }

    override fun onCleared() = application.unbindService(this)

    fun isWalletServiceConnected() = connectionState.value is ServiceConnectionState.Connected

    suspend fun doOnWalletServiceConnected(action: suspend (walletService: TariWalletService) -> Unit) = withContext(Dispatchers.IO) {
        connectionState.firstOrNull { it is ServiceConnectionState.Connected }
            ?.safeCastTo<ServiceConnectionState.Connected>()?.service
            ?.let {
                action(it)
            } ?: logger.i("Wallet service is not connected")
    }

    suspend fun <T> doOnWalletServiceConnectedWithValue(action: suspend (walletService: TariWalletService) -> T): T = withContext(Dispatchers.IO) {
        connectionState.firstOrNull { it is ServiceConnectionState.Connected }
            ?.safeCastTo<ServiceConnectionState.Connected>()?.service
            ?.let {
                action(it)
            } ?: error("Wallet service is not connected")
    }
}

sealed class ServiceConnectionState {
    data object NotYetConnected : ServiceConnectionState()
    data object Disconnected : ServiceConnectionState()
    data class Connected(val service: TariWalletService) : ServiceConnectionState()
}