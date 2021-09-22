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
import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import java.io.File

/**
 * Utilized to asynchoronously manage the sometimes-long-running task of instantiation and start-up
 * of the Tor proxy and the FFI wallet.
 *
 * @author The Tari Development Team
 */
internal class WalletManager(
    private val context: Context,
    private val walletFilesDirPath: String,
    private val walletLogFilePath: String,
    private val torManager: TorProxyManager,
    private val sharedPrefsWrapper: SharedPrefsRepository,
    private val baseNodeSharedRepository: BaseNodeSharedRepository,
    private val seedPhraseRepository: SeedPhraseRepository,
    private val baseNodes: BaseNodes,
    private val torConfig: TorConfig
) {

    private var logFileObserver: LogFileObserver? = null

    init {
        // post initial wallet state
        EventBus.walletState.post(WalletState.NotReady)
    }

    /**
     * Start tor and init wallet.
     */
    @Synchronized
    fun start() {
        Thread {
            torManager.run()
        }.start()
        // subscribe to Tor proxy state changes
        EventBus.torProxyState.subscribe(this, this::onTorProxyStateChanged)
    }

    /**
     * Deinit the wallet and shutdown Tor.
     */
    @Synchronized
    fun stop() {
        // destroy FFI wallet object
        FFIWallet.instance?.destroy()
        FFIWallet.instance = null
        EventBus.walletState.post(WalletState.NotReady)
        // stop tor proxy
        EventBus.torProxyState.unsubscribe(this)
        torManager.shutdown()
    }

    @SuppressLint("CheckResult")
    private fun onTorProxyStateChanged(torProxyState: TorProxyState) {
        Logger.d("Tor proxy state has changed: $torProxyState.")
        if (torProxyState is TorProxyState.Running) {
            if (EventBus.walletState.publishSubject.value == WalletState.NotReady ||
                EventBus.walletState.publishSubject.value is WalletState.Failed
            ) {
                Logger.d("Initialize wallet.")
                EventBus.walletState.post(WalletState.Initializing)
                Thread {
                    try {
                        initWallet()
                        EventBus.walletState.post(WalletState.Running)
                    } catch (e: Exception) {
                        EventBus.walletState.post(WalletState.Failed(e))
                    }
                }.start()
            }
        }
    }

    /**
     * Instantiates the Tor transport for the wallet.
     */
    private fun getTorTransport(): FFITransportType {
        val cookieFile = File(torConfig.cookieFilePath)
        val cookieString: ByteArray = cookieFile.readBytes()
        val torCookie = FFIByteVector(cookieString)
        return FFITransportType(
            NetAddressString(
                torConfig.controlHost,
                torConfig.controlPort
            ),
            torCookie,
            torConfig.connectionPort,
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
            Constants.Wallet.discoveryTimeoutSec,
            Constants.Wallet.storeAndForwardMessageDurationSec,
            Network.WEATHERWAX.uriComponent,
        )
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
            sharedPrefsWrapper.emojiId = publicKeyFFI.getEmojiId()
            publicKeyFFI.destroy()
        }
    }

    /**
     * Initializes the wallet and sets the singleton instance in the wallet companion object.
     */
    private fun initWallet() {
        if (FFIWallet.instance == null) {
            // store network info in shared preferences if it's a new wallet
            val isNewInstallation = !WalletUtil.walletExists(context)
            val wallet = FFIWallet(
                sharedPrefsWrapper,
                seedPhraseRepository,
                getCommsConfig(),
                walletLogFilePath
            )
            FFIWallet.instance = wallet
            if (isNewInstallation) {
                sharedPrefsWrapper.network = Constants.Wallet.network
                FFIWallet.instance?.setKeyValue(
                    WalletService.Companion.KeyValueStorageKeys.NETWORK,
                    Constants.Wallet.network.uriComponent
                )
            } else if (sharedPrefsWrapper.isRestoredWallet && sharedPrefsWrapper.network == null) {
                sharedPrefsWrapper.network = try {
                    Network.from(
                        FFIWallet.instance?.getKeyValue(
                            WalletService.Companion.KeyValueStorageKeys.NETWORK
                        ) ?: ""
                    )
                } catch (exception: Exception) {
                    null
                }
            }
            startLogFileObserver()
            val currentBaseNode = baseNodeSharedRepository.currentBaseNode
            if (currentBaseNode != null) {
                baseNodes.addIntoWallet(currentBaseNode)
            } else {
                baseNodes.setNextBaseNode()
            }
            saveWalletPublicKeyHexToSharedPrefs()
        }
    }

}
