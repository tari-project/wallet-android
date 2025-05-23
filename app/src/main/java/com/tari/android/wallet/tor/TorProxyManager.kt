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

import android.app.Service
import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the installation and the running of the Tor proxy.
 * https://2019.www.torproject.org/docs/tor-manual.html.en
 *
 * @author The Tari Development Team
 */
@Singleton
class TorProxyManager @Inject constructor(
    private val context: Context,
    private val torPrefRepository: TorPrefRepository,
    private val torConfig: TorConfig,
    private val torProxyControl: TorProxyControl,
    private val torProxyStateHandler: TorProxyStateHandler,
) {

    companion object {
        const val TOR_DATA_DIRECTORY_NAME = "tor_data"
    }

    private val appCacheHome = context.getDir(TOR_DATA_DIRECTORY_NAME, Service.MODE_PRIVATE)
    private val logger
        get() = Logger.t(TorProxyManager::class.java.simpleName)

    init {
        torProxyStateHandler.updateState(TorProxyState.NotReady)
    }

    @Synchronized
    fun run() {
        Thread {
            try {
                // start monitoring Tor on a separate thread
                Thread { torProxyControl.startMonitoringTor() }.start()

                installTorResources()

                val torCmdString = "${torPrefRepository.torBinPath} DataDirectory ${appCacheHome.absolutePath} " +
                        "--defaults-torrc ${torPrefRepository.torrcBinPath}.custom"

                exec(torCmdString)
            } catch (throwable: Throwable) {
                logger.e(throwable, "Tor process launch failed")
                torProxyStateHandler.updateState(TorProxyState.Failed(throwable))
            }
        }.start()
    }

    fun shutdown() {
        torProxyControl.shutdownTor()
        torProxyStateHandler.updateState(TorProxyState.NotReady)
    }

    private fun installTorResources() {
        // install Tor geoip resources
        val torResourceInstaller = TorResourceInstaller(context, torPrefRepository, torConfig)
        torResourceInstaller.installResources()
        torPrefRepository.torBinPath = torResourceInstaller.fileTor.absolutePath
        torPrefRepository.torrcBinPath = torResourceInstaller.fileTorrc.absolutePath
    }

    /**
     * Executes shell command.
     */
    private fun exec(command: String) {
        try {
            val response = Runtime.getRuntime().exec("killall tor")
            response.waitFor()
            val responseText = BufferedReader(InputStreamReader(response.inputStream)).use(BufferedReader::readText)
            println(responseText)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        val process = Runtime.getRuntime().exec(command)
        logger.i("Tor process started")
        torProxyStateHandler.updateState(TorProxyState.Initializing(bootstrapStatus = null))
        val response = BufferedReader(InputStreamReader(process.inputStream)).use(BufferedReader::readText)
        logger.i("Tor proxy response: $response")
        process.waitFor()
        logger.i("Tor proxy stopped")
    }
}