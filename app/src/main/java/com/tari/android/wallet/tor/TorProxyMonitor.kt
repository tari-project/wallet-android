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
package com.tari.android.wallet.tor

import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.EventBus
import net.freehaven.tor.control.TorControlConnection
import java.io.File
import java.net.Socket
import java.util.*
import kotlin.concurrent.schedule

/**
 * Monitors the state of the Tor proxy.
 *
 * @author The Tari Development Team
 */
internal class TorProxyMonitor(private val torConfig: TorConfig) {

    private val initializationCheckTimeoutMs = 15000L
    private val initializationCheckRetryPeriodMs = 500L
    private val statusCheckPeriodMs = 5000L
    private var monitoringStartTimeMs = 0L
    private val timer = Timer("Tor Proxy Monitor Timer", false)

    private lateinit var socket: Socket
    private lateinit var controlConnection: TorControlConnection
    private var currentState = EventBus.torProxyStateSubject.value

    @Synchronized
    fun startMonitoringTor() {
        connectToTorProxy()
    }

    private fun connectToTorProxy() {
        monitoringStartTimeMs = System.currentTimeMillis()
        try {
            socket = Socket(torConfig.controlHost, torConfig.controlPort)
            controlConnection = TorControlConnection(socket)
            val cookieFileBytes = File(torConfig.cookieFilePath).readBytes()
            controlConnection.authenticate(cookieFileBytes)
            checkTorStatus()
        } catch (throwable: Throwable) {
            val initializationElapsedMs = System.currentTimeMillis() - monitoringStartTimeMs
            if (initializationElapsedMs > initializationCheckTimeoutMs) {
                Logger.e(
                    "Failed to connect to Tor proxy, timed out: %s",
                    throwable.message
                )
                updateState(TorProxyState.Failed)
            } else {
                Logger.e(
                    "Failed to connect to Tor proxy, will retry: %s",
                    throwable.message
                )
                // timer - retry
                timer.schedule(initializationCheckRetryPeriodMs) {
                    connectToTorProxy()
                }
            }
        }
    }

    private fun checkTorStatus() {
        try {
            val bootstrapStatus = isTorRunning(controlConnection)
            if (bootstrapStatus != null) {
                updateState(TorProxyState.Running(bootstrapStatus))
                // schedule timer
                timer.schedule(statusCheckPeriodMs) {
                    checkTorStatus()
                }
            } else {
                updateState(TorProxyState.Failed)
            }
        } catch (throwable: Throwable) {
            Logger.e("Tor proxy has failed: %s", throwable.message)
            updateState(TorProxyState.Failed)
        }
    }

    private fun isTorRunning(controlConnection: TorControlConnection?): TorBootstrapStatus? {
        val phaseLogLine = controlConnection?.getInfo("status/bootstrap-phase") ?: return null
        return TorBootstrapStatus.from(phaseLogLine)
    }

    private fun updateState(newState: TorProxyState) {
        if (currentState != newState) {
            currentState = newState
            EventBus.postTorProxyState(currentState!!)
        }
    }

}