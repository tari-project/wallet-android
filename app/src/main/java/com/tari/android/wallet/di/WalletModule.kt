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
package com.tari.android.wallet.di

import android.content.Context
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import dagger.Module
import dagger.Provides
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject wallet-related dependencies.
 *
 * @author The Tari Development Team
 */
@Module
internal class WalletModule {

    object FieldName {
        const val walletFilesDirPath = "wallet_files_dir_path"
        const val walletLogFilePath = "wallet_log_file_path"
    }

    private val logFilePrefix = "tari_wallet_"

    /**
     * The directory in which the wallet files reside.
     */
    @Provides
    @Named(FieldName.walletFilesDirPath)
    internal fun provideWalletFilesDirPath(context: Context): String = context.filesDir.absolutePath

    /**
     * FFI log file path.
     */
    @Provides
    @Named(FieldName.walletLogFilePath)
    internal fun provideWalletLogFilePath(@Named(FieldName.walletFilesDirPath) walletFilesDirPath: String): String {
        val timeStamp = SimpleDateFormat("YYYYMMdd", Locale.getDefault()).format(Date()) +
                "_${System.currentTimeMillis()}"

        return "$walletFilesDirPath/$logFilePrefix$timeStamp.log"
    }

    /**
     * Creates a new private key & stores if it doesn't exist.
     */
    private fun getPrivateKeyHexString(sharedPrefsWrapper: SharedPrefsWrapper): HexString {
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
     * Provides transport for wallet to use
     */
    @Provides
    @Singleton
    internal fun provideTorTransport(
        torConfig: TorConfig
    ): FFITransportType {


        val cookieFile = File(torConfig.cookieFilePath)
        var cookieString = ByteArray(0)
        if (cookieFile.exists()) {
            cookieString = cookieFile.readBytes()
        }

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
     * Provides CommsConfig object for wallet configuration.
     */
    @Provides
    @Singleton
    internal fun provideCommsConfig(
        sharedPrefsWrapper: SharedPrefsWrapper,
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String,
        transport: FFITransportType
    ): FFICommsConfig {
        return FFICommsConfig(
            NetAddressString(
                "127.0.0.1",
                39069
            ).toString(),
            //transport.getAddress(),
            transport,
            Constants.Wallet.WALLET_DB_NAME,
            walletFilesDirPath,
            FFIPrivateKey((getPrivateKeyHexString(sharedPrefsWrapper)))
        )
    }

    /**
     * Provides wallet object.
     */
    @Provides
    @Singleton
    internal fun provideTestWallet(
        commsConfig: FFICommsConfig,
        @Named(FieldName.walletLogFilePath) logFilePath: String,
        sharedPrefsWrapper: SharedPrefsWrapper
    ): FFITestWallet {
        if (FFITestWallet.instance == null) {
            val wallet = FFITestWallet(commsConfig, logFilePath)
            FFITestWallet.instance = wallet
            // set shared preferences values after instantiation
            val publicKeyFFI = wallet.getPublicKey()
            sharedPrefsWrapper.publicKeyHexString = publicKeyFFI.toString()
            sharedPrefsWrapper.emojiId = publicKeyFFI.getEmojiNodeId()
            publicKeyFFI.destroy()
            //Todo: Below needs to be run once on first run

            val baseNodeKeyFFI =
                FFIPublicKey(HexString("90d8fe54c377ecabff383f7d8f0ba708c5b5d2a60590f326fbf1a2e74ea2441f"))
            val baseNodeAddress =
                "/onion3/plvcskybsckbfeubywjbmpnbm4kjqm2ip6kbwimakaim6xyucydpityd:18001"
            wallet.addBaseNodePeer(baseNodeKeyFFI, baseNodeAddress)

            baseNodeKeyFFI.destroy()


        }
        return FFITestWallet.instance!!
    }

}