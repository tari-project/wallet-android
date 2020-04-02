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
package com.tari.android.wallet.application

import android.annotation.SuppressLint
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFITransportType
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.LogFileObserver
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.nullptr
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyMonitor
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import java.io.File

/**
 * Utilized to asynchoronously manage the sometimes-long-running task of instantiation and start-up
 * of the Tor proxy and the FFI wallet.
 *
 * @author The Tari Development Team
 */
internal class WalletManager(
    private val walletFilesDirPath: String,
    private val walletLogFilePath: String,
    private val torProxyManager: TorProxyManager,
    private val torProxyMonitor: TorProxyMonitor,
    private val sharedPrefsWrapper: SharedPrefsWrapper,
    private val torConfig: TorConfig
) {

    private var logFileObserver: LogFileObserver? = null

    init {
        // post initial wallet state
        EventBus.postWalletState(WalletState.NOT_READY)
    }

    /**
     * Only visible function. Can be run from any thead.
     */
    fun start() {
        /**
         * Run Tor on a separate thread.
         */
        Thread {
            torProxyManager.runTorProxy()
        }.start()
        /**
         * Start monitoring Tor on a separate thread.
         */
        Thread {
            torProxyMonitor.startMonitoringTor()
        }.start()

        /**
         * Subscribe to Tor proxy state changes.
         */
        EventBus.subscribeToTorProxyState(this) { state ->
            onTorProxyStateChanged(state)
        }

    }

    @SuppressLint("CheckResult")
    private fun onTorProxyStateChanged(torProxyState: TorProxyState) {
        Logger.d("Tor proxy state has changed: $torProxyState.")
        if (torProxyState is TorProxyState.Running) {
            if (EventBus.walletStateSubject.value == WalletState.NOT_READY) {
                Logger.d("Initialize wallet.")
                EventBus.postWalletState(WalletState.INITIALIZING)
                Thread {
                    initWallet()
                    EventBus.postWalletState(WalletState.RUNNING)
                }.start()
            }
        }
    }

    /**
     * Instantiates the Tor transport for the wallet.
     */
    private fun getTorTransport(): FFITransportType {
        val cookieFile = File(torConfig.cookieFilePath)
        val cookieString = cookieFile.readBytes()
        val torCookie = FFIByteVector(cookieString)
        var torIdentity = FFIByteVector(nullptr)
        if (torConfig.identity.isNotEmpty()) {
            torIdentity.destroy()
            torIdentity = FFIByteVector(torConfig.identity)
        }
        return FFITransportType(
            NetAddressString(
                torConfig.controlHost,
                torConfig.controlPort
            ),
            torConfig.connectionPort,
            torCookie,
            torIdentity,
            torConfig.sock5Username,
            torConfig.sock5Password
        )
    }

    /**
     * Instantiates the comms configuration for the wallet.
     */
    private fun getCommsConfig(): FFICommsConfig {
        return FFICommsConfig(
            NetAddressString(
                "127.0.0.1",
                39069
            ).toString(),
            getTorTransport(),
            Constants.Wallet.walletDBName,
            walletFilesDirPath,
            FFIPrivateKey((getWalletPrivateKeyHexString(sharedPrefsWrapper)))
        )
    }

    /**
     * If a private key is saved in the shared prefs, returns the saved value,
     * if not, generates a net private key, saves it in the shared prefs & returns it.
     */
    private fun getWalletPrivateKeyHexString(sharedPrefsWrapper: SharedPrefsWrapper): HexString {
        var hexString = sharedPrefsWrapper.privateKeyHexString
        if (hexString == null) {
            val privateKeyFFI = FFIPrivateKey()
            hexString = privateKeyFFI.toString()
            privateKeyFFI.destroy()
            sharedPrefsWrapper.privateKeyHexString = hexString
        }
        return HexString(hexString)
    }

    /**
     * Adds the default base node and saves the base node config values into the shared prefs.
     * Will only run once when the wallet is created for the first time.
     */
    private fun addBaseNode() {
        // add base node
        if (sharedPrefsWrapper.baseNodePublicKeyHex == null) {
            sharedPrefsWrapper.baseNodePublicKeyHex = Constants.Wallet.baseNodePublicKeyHex
            sharedPrefsWrapper.baseNodeAddress = Constants.Wallet.baseNodeAddress

            val baseNodeKeyFFI = FFIPublicKey(HexString(Constants.Wallet.baseNodePublicKeyHex))
            val baseNodeAddress = Constants.Wallet.baseNodeAddress
            FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, baseNodeAddress)
            baseNodeKeyFFI.destroy()
        }
    }

    /**
     * Starts the log file observer only in debug mode.
     * Will skip if the app is in release config.
     */
    private fun startLogFileObserver() {
        if (BuildConfig.DEBUG) {
            logFileObserver = LogFileObserver(walletLogFilePath)
            logFileObserver?.startWatching()
        }
    }

    /**
     * Stores wallet's public key hex and emoji id's into the shared prefs
     * for future convenience.
     */
    private fun saveWalletPublicKeyHexToSharedPrefs() {
        // set shared preferences values after instantiation
        FFIWallet.instance?.getPublicKey()?.let { publicKeyFFI ->
            sharedPrefsWrapper.publicKeyHexString = publicKeyFFI.toString()
            sharedPrefsWrapper.emojiId = publicKeyFFI.getEmojiNodeId()
            publicKeyFFI.destroy()
        }
    }

    /**
     * Initializes the wallet and sets the singleton instance in the wallet companion object.
     */
    private fun initWallet() {
        if (FFIWallet.instance == null) {
            val wallet = FFIWallet(
                getCommsConfig(),
                walletLogFilePath
            )
            FFIWallet.instance = wallet
            startLogFileObserver()
            addBaseNode()
            saveWalletPublicKeyHexToSharedPrefs()
        }
    }

}
