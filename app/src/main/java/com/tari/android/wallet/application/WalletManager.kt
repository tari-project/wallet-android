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
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import org.apache.commons.io.IOUtils
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
    private val sharedPrefsWrapper: SharedPrefsWrapper,
    private val torConfig: TorConfig
) {

    private var logFileObserver: LogFileObserver? = null
    private lateinit var baseNodeIterator: Iterator<Triple<String, String, String>>

    init {
        // post initial wallet state
        EventBus.postWalletState(WalletState.NOT_READY)
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
        EventBus.subscribeToTorProxyState(this, this::onTorProxyStateChanged)
    }

    /**
     * Deinit the wallet and shutdown Tor.
     */
    @Synchronized
    fun stop() {
        // destroy FFI wallet object
        FFIWallet.instance?.destroy()
        FFIWallet.instance = null
        EventBus.postWalletState(WalletState.NOT_READY)
        // stop tor proxy
        EventBus.unsubscribeFromTorProxyState(this)
        torManager.shutdown()
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
        return FFITransportType(
            NetAddressString(
                torConfig.controlHost,
                torConfig.controlPort
            ),
            torConfig.connectionPort,
            torCookie,
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
            Constants.Wallet.storeAndForwardMessageDurationSec
        )
    }

    /**
     * Returns the list of base nodes in the resource file base_nodes.txt as pairs of
     * ({name}, {public_key_hex}, {public_address}).
     */
    private val baseNodeList by lazy {
        val fileContent = IOUtils.toString(
            context.resources.openRawResource(R.raw.base_nodes),
            "UTF-8"
        )
        val baseNodes = mutableListOf<Triple<String, String, String>>()
        val regex = Regex("(.+::[A-Za-z0-9]{64}::/onion3/[A-Za-z0-9]+:[\\d]+)")
        regex.findAll(fileContent).forEach { matchResult ->
            val tripleString = matchResult.value.split("::")
            baseNodes.add(
                Triple(
                    tripleString[0],
                    tripleString[1],
                    tripleString[2]
                )
            )
        }
        baseNodes.shuffle()
        baseNodes
    }

    /**
     * Select a base node randomly from the list of base nodes in base_nodes.tx, and sets
     * the wallet and stored the values in shared prefs.
     */
    fun setNextBaseNode() {
        if (!this::baseNodeIterator.isInitialized || !baseNodeIterator.hasNext()) {
            baseNodeIterator = baseNodeList.iterator()
        }
        val baseNode = baseNodeIterator.next()
        val name = baseNode.first
        val publicKeyHex = baseNode.second
        val address = baseNode.third
        sharedPrefsWrapper.baseNodeLastSyncResult = null
        sharedPrefsWrapper.baseNodeName = name
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        sharedPrefsWrapper.baseNodeIsUserCustom = false

        val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
        FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, address)
        baseNodeKeyFFI.destroy()
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
            // don't change the base node if it's a custom base node entered by the user
            if (sharedPrefsWrapper.baseNodeIsUserCustom) {
                val publicKeyHex = sharedPrefsWrapper.baseNodePublicKeyHex
                val address = sharedPrefsWrapper.baseNodeAddress
                if (publicKeyHex == null || address == null) {
                    // there's something unexpected with the data, use Tari base nodes
                    setNextBaseNode()
                } else {
                    // data is ok, set custom user base node
                    val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
                    FFIWallet.instance?.addBaseNodePeer(baseNodeKeyFFI, address)
                    baseNodeKeyFFI.destroy()
                }
            } else {
                setNextBaseNode()
            }
            saveWalletPublicKeyHexToSharedPrefs()
        }
    }

}
