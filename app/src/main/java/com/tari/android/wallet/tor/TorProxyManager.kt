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
import com.tari.android.wallet.util.SharedPrefsWrapper
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import net.freehaven.tor.control.TorControlConnection
import org.torproject.android.binary.TorResourceInstaller
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket

/**
 * Manages the installation and the running of the Tor proxy.
 *
 * @author The Tari Development Team
 */
internal class TorProxyManager(
    private val context: Context,
    private val sharedPrefsWrapper: SharedPrefsWrapper,
    private val torConfig: TorConfig
) {

    companion object {
        const val torDataDirectoryName = "tor_data"
    }

    private fun installTorResources() {
        val torBinPath = sharedPrefsWrapper.torBinPath
        if (torBinPath != null) {
            val torBinFile = File(torBinPath)
            if (torBinFile.exists() && torBinFile.canExecute()) {
                return
            }
        }
        val torResourceInstaller = TorResourceInstaller(context, context.filesDir)
        torResourceInstaller.installGeoIPResources()
        val torBinary = torResourceInstaller.getTorBinaryFile()
        sharedPrefsWrapper.torBinPath = torBinary.absolutePath
    }

    /**
     * Executes shell command.
     */
    private fun exec(command: String) {
        try {
            // execute the command
            val process = Runtime.getRuntime().exec(command)
            Logger.d("Tor command executed: %s", command)
            val response = BufferedReader(
                InputStreamReader(process.inputStream)
            ).use(BufferedReader::readText)
            Logger.d("Tor proxy response: %s", response)
            process.waitFor()
            // Tor proxy is down
            throw RuntimeException("Tor proxy is down.")
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    fun runTorProxy() {
        Logger.d("Starting Tor")
        installTorResources()

        val appCacheHome = context.getDir(torDataDirectoryName, Service.MODE_PRIVATE)
        val torCmdString =
            "${sharedPrefsWrapper.torBinPath} DataDirectory ${appCacheHome.absolutePath} " +
                    "--allow-missing-torrc --ignore-missing-torrc " +
                    "--clientonly 1 " +
                    "--socksport ${torConfig.proxyPort} " +
                    "--controlport ${torConfig.controlHost}:${torConfig.controlPort} " +
                    "--CookieAuthentication 1 " +
                    "--Socks5ProxyUsername ${torConfig.sock5Username} " +
                    "--Socks5ProxyPassword ${torConfig.sock5Password} " +
                    "--clientuseipv6 1"
        exec(torCmdString)
    }

    fun start(timeout: Long, listener: TorProxyListener) {
        // Run Tor
        Thread { runTorProxy() }.start()

        // Start checking for Tor proxy on a different thread
        Thread {
            val success = verifyTorIsRunning(timeout)
            listener.onTorProxyInitResult(success)
        }.start()
    }

    private fun verifyTorIsRunning(timeout: Long): Boolean {
        var controlConnection: TorControlConnection? = null
        var socket: Socket? = null
        try {
            // We will check every second to see if boot strapping has finally finished
            for (secondsWaited in 0 until timeout) {
                try {
                    if (controlConnection == null) {
                        socket = Socket(torConfig.controlHost, torConfig.controlPort)
                        controlConnection = connectToTorControlSocket(socket)
                    }
                } catch (ex: Exception) {
                    Logger.d("Tor control connection failed with error %s", ex.message)
                }

                if (isTorRunning(controlConnection)) {
                    Logger.d("Tor start success")
                    return true
                }
                sleep(1000)
            }
            return false
        } finally {
            try {
                socket?.close()
            } catch (ignore: Exception) {
            }
        }
    }

    /**
     * Finds Tor control connection by trying to connect.
     */
    private fun connectToTorControlSocket(socket: Socket): TorControlConnection? {
        val connection: TorControlConnection
        try {
            connection =
                TorControlConnection(socket)
            val file = File(torConfig.cookieFilePath)
            connection.authenticate(read(file))
        } catch (e: IOException) {
            throw e
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw IOException("Failed to read control port:  ${torConfig.controlPort}")
        }
        return connection
    }

    private fun isTorRunning(controlConnection: TorControlConnection?): Boolean {
        if (controlConnection == null) {
            return false
        }
        try {
            val phase = controlConnection.getInfo("status/bootstrap-phase")
            if (phase != null && phase.contains("PROGRESS=100")) {
                Logger.d("Tor has already bootstrapped")
                return true
            }
        } catch (e: IOException) {
            Logger.d("Control connection is not responding properly to getInfo", e)
        }
        return false
    }

    private fun read(file: File): ByteArray? {
        val bytes = ByteArray(file.length().toInt())
        val inputStream = FileInputStream(file)
        return inputStream.use { inputStream ->
            var offset = 0
            while (offset < bytes.size) {
                val read = inputStream.read(bytes, offset, bytes.size - offset)
                if (read == -1) throw EOFException()
                offset += read
            }
            bytes
        }
    }
}

interface TorProxyListener {
    fun onTorProxyInitResult(success: Boolean)
}