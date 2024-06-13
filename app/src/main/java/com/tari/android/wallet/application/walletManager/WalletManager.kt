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
package com.tari.android.wallet.application.walletManager

import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFITariTransportConfig
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.LogFileObserver
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.service.service.WalletService
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilized to asynchronous manage the sometimes-long-running task of instantiation and start-up
 * of the Tor proxy and the FFI wallet.
 *
 * @author The Tari Development Team
 */
@Singleton
class WalletManager @Inject constructor(
    private val walletConfig: WalletConfig,
    private val torManager: TorProxyManager,
    private val corePrefRepository: CorePrefRepository,
    private val baseNodePrefRepository: BaseNodePrefRepository,
    private val seedPhraseRepository: SeedPhraseRepository,
    private val networkPrefRepository: NetworkPrefRepository,
    private val tariSettingsPrefRepository: TariSettingsPrefRepository,
    private val securityPrefRepository: SecurityPrefRepository,
    private val baseNodesManager: BaseNodesManager,
    private val torConfig: TorConfig,
    private val walletStateHandler: WalletStateHandler,
    private val torProxyStateHandler: TorProxyStateHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private var logFileObserver: LogFileObserver? = null
    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    /**
     * Start tor and init wallet.
     */
    @Synchronized
    fun start() {
        torManager.run()

        applicationScope.launch {
            torProxyStateHandler.doOnTorReadyForWallet {
                startWallet()
            }
        }
    }

    /**
     * DeInit the wallet and shutdown Tor.
     */
    @Synchronized
    fun stop() {
        // destroy FFI wallet object
        FFIWallet.instance?.destroy()
        FFIWallet.instance = null
        walletStateHandler.setWalletState(WalletState.NotReady)
        // stop tor proxy
        torManager.shutdown()
        walletStateHandler.setWalletState(WalletState.NotReady) // todo Don't understand why it's twice
    }

    fun onWalletStarted() {
        walletStateHandler.setWalletState(WalletState.Running)
    }

    /**
     * Instantiates the comms configuration for the wallet.
     */
    fun getCommsConfig(): FFICommsConfig = FFICommsConfig(
        publicAddress = NetAddressString(address = "127.0.0.1", port = 39069).toString(),
        transport = getTorTransport(),
        databaseName = walletConfig.walletDBName,
        datastorePath = walletConfig.getWalletFilesDirPath(),
        discoveryTimeoutSec = Constants.Wallet.discoveryTimeoutSec,
        safMessageDurationSec = Constants.Wallet.storeAndForwardMessageDurationSec,
    )

    private fun startWallet() {
        if (walletStateHandler.walletState.value is WalletState.NotReady || walletStateHandler.walletState.value is WalletState.Failed) {
            logger.i("Initialize wallet started")
            walletStateHandler.setWalletState(WalletState.Initializing)
            applicationScope.launch {
                try {
                    initWallet()
                    walletStateHandler.setWalletState(WalletState.Started)
                    logger.i("Wallet was started")
                } catch (e: Exception) {
                    val oldCode = walletStateHandler.walletState.value.errorCode
                    val newCode = e.safeCastTo<FFIException>()?.error?.code

                    if (oldCode == null || oldCode != newCode) {
                        logger.e(e, "Wallet was failed")
                    }
                    walletStateHandler.setWalletState(WalletState.Failed(e))
                }
            }.start()
        }
    }

    /**
     * Instantiates the Tor transport for the wallet.
     */
    private fun getTorTransport(): FFITariTransportConfig {
        val cookieFile = File(torConfig.cookieFilePath)
        if (!cookieFile.exists()) {
            cookieFile.createNewFile()
        }
        val cookieString: ByteArray = cookieFile.readBytes()
        val torCookie = FFIByteVector(cookieString)
        return FFITariTransportConfig(
            controlAddress = NetAddressString(torConfig.controlHost, torConfig.controlPort),
            torCookie = torCookie,
            torPort = torConfig.connectionPort,
            socksUsername = torConfig.sock5Username,
            socksPassword = torConfig.sock5Password,
        )
    }

    /**
     * Starts the log file observer only in debug mode.
     * Will skip if the app is in release config.
     */
    private fun startLogFileObserver() {
        if (BuildConfig.DEBUG) {
            logFileObserver = LogFileObserver(walletConfig.getWalletLogFilePath())
            logFileObserver?.startWatching()
        }
    }

    /**
     * Stores wallet's public key hex and emoji id's into the shared prefs
     * for future convenience.
     */
    private fun saveWalletPublicKeyHexToSharedPrefs() {
        // set shared preferences values after instantiation
        FFIWallet.instance?.getWalletAddress()?.let { ffiTariWalletAddress ->
            corePrefRepository.publicKeyHexString = ffiTariWalletAddress.toString()
            corePrefRepository.emojiId = ffiTariWalletAddress.getEmojiId()
            ffiTariWalletAddress.destroy()
        }
    }

    /**
     * Initializes the wallet and sets the singleton instance in the wallet companion object.
     */
    private fun initWallet() {
        if (FFIWallet.instance == null) {
            // store network info in shared preferences if it's a new wallet
            val isNewInstallation = !WalletUtil.walletExists(walletConfig)
            val wallet = FFIWallet(
                sharedPrefsRepository = corePrefRepository,
                securityPrefRepository = securityPrefRepository,
                seedPhraseRepository = seedPhraseRepository,
                networkRepository = networkPrefRepository,
                commsConfig = getCommsConfig(),
                logPath = walletConfig.getWalletLogFilePath(),
            )
            FFIWallet.instance = wallet
            if (isNewInstallation) {
                FFIWallet.instance?.setKeyValue(
                    key = WalletService.Companion.KeyValueStorageKeys.NETWORK,
                    value = networkPrefRepository.currentNetwork.network.uriComponent,
                )
            } else if (tariSettingsPrefRepository.isRestoredWallet && networkPrefRepository.ffiNetwork == null) {
                networkPrefRepository.ffiNetwork = try {
                    Network.from(FFIWallet.instance?.getKeyValue(WalletService.Companion.KeyValueStorageKeys.NETWORK) ?: "")
                } catch (exception: Exception) {
                    null
                }
            }
            startLogFileObserver()

            baseNodesManager.refreshBaseNodeList()
            val currentBaseNode = baseNodePrefRepository.currentBaseNode
            if (currentBaseNode != null) {
                baseNodesManager.startSync()
            } else {
                baseNodesManager.setNextBaseNode()
                baseNodesManager.startSync()
            }
            saveWalletPublicKeyHexToSharedPrefs()
        }
    }
}
