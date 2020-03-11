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
    @Singleton
    internal fun provideWalletFilesDirPath(context: Context): String = context.filesDir.absolutePath

    /**
     * FFI log file path.
     */
    @Provides
    @Named(FieldName.walletLogFilePath)
    @Singleton
    internal fun provideWalletLogFilePath(@Named(FieldName.walletFilesDirPath) walletFilesDirPath: String): String {
        val timeStamp = SimpleDateFormat("YYYYMMdd", Locale.getDefault()).format(Date()) +
                "_${System.currentTimeMillis()}"
        val logFilePath = "$walletFilesDirPath/$logFilePrefix$timeStamp.log"
        val logFile = File(logFilePath)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        return logFilePath
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
            Constants.Wallet.walletDBName,
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

            // add base node
            if (sharedPrefsWrapper.baseNodePublicKeyHex == null) {
                sharedPrefsWrapper.baseNodePublicKeyHex = Constants.Wallet.baseNodePublicKeyHex
                sharedPrefsWrapper.baseNodeAddress = Constants.Wallet.baseNodeAddress

                val baseNodeKeyFFI = FFIPublicKey(HexString(Constants.Wallet.baseNodePublicKeyHex))
                val baseNodeAddress = Constants.Wallet.baseNodeAddress
                wallet.addBaseNodePeer(baseNodeKeyFFI, baseNodeAddress)
                baseNodeKeyFFI.destroy()
            }
        }
        return FFITestWallet.instance!!
    }

}