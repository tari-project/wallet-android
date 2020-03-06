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
package com.tari.android.wallet.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.torproject.android.binary.TorResourceInstaller
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "TorService"

const val DIRECTORY_TOR_DATA = "tordata"

class TorService : Service() {

    private val listeners = mutableListOf<TariTorServiceListener>()

    private var torBinPath: String? = null
    private var installSuccess: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        installTor()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun installTor() {
        try {
            val torResourceInstaller = TorResourceInstaller(this, filesDir)
            val fileTorBin = torResourceInstaller.installResources()
            installSuccess = fileTorBin != null && fileTorBin.canExecute()

            if (installSuccess) {
                Log.d(TAG, "TOR install success")
                torBinPath = fileTorBin.canonicalPath
            }
        } catch (t: Throwable) {
            Log.e(TAG, "TOR install error: " + t.message)
        }
    }

    private fun runTor(
        socksPort: Int,
        controlHost: String,
        controlPort: Int,
        sock5Username: String,
        sock5Password: String
    ) {
        val appCacheHome =
            getDir(DIRECTORY_TOR_DATA, MODE_PRIVATE)

        val torCmdString = "${torBinPath!!} DataDirectory ${appCacheHome.canonicalPath} " +
                "--allow-missing-torrc --ignore-missing-torrc " +
                "--clientonly 1 " +
                "--socksport $socksPort " +
                "--controlport $controlHost:$controlPort " +
                "--CookieAuthentication 1 " +
                "--Socks5ProxyUsername $sock5Username " +
                "--Socks5ProxyPassword $sock5Password " +
                "--clientuseipv6 1"

        var output = ""
        try {
            output = exec(torCmdString)
        } catch (e: Throwable) {
            Log.e(TAG, "Error while running TOR: " + e.message)
            notifyError("Error while running TOR: " + e.message)
        }

        Log.d(TAG, "Tor finished with output $output")
    }

    private fun exec(command: String): String {
        return try { // Executes the command.
            val process =
                Runtime.getRuntime().exec(command)
            // Reads stdout.
            val response = BufferedReader(
                InputStreamReader(process.inputStream)
            ).use(BufferedReader::readText)
            // Waits for the command to finish.
            process.waitFor()
            response
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    private fun checkAndNotifyInstallSuccess(): Boolean {
        if (!installSuccess) {
            notifyError("Tari install has failed, check logs for more details...")
        }
        return installSuccess
    }

    private fun notifyError(error: String?) {
        error?.let { err -> listeners.forEach { it.onTorServiceError(err) } }
    }

    private val binder = object : TariTorService.Stub() {
        override fun registerListener(listener: TariTorServiceListener) {
            listeners.add(listener)
            listener.asBinder().linkToDeath({
                listeners.remove(listener)
            }, 0)
        }

        override fun unregisterListener(listener: TariTorServiceListener) {
            listeners.remove(listener)
        }

        override fun start(
            socksPort: Int,
            controlHost: String,
            controlPort: Int,
            sock5Username: String,
            sock5Password: String
        ) {
            if (!checkAndNotifyInstallSuccess()) return

            Thread {
                runTor(
                    socksPort,
                    controlHost,
                    controlPort,
                    sock5Username,
                    sock5Password
                )
            }.start()
        }
    }
}