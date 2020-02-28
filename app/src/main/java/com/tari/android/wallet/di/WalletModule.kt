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
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject wallet-related dependencies.
 *
 * @author The Tari Development Team
 */
@Module
class WalletModule {

    object FieldName {
        const val walletFilesDirPath = "wallet_files_dir_path"
        const val walletLogFilePath = "wallet_log_file_path"
        const val emojiId = "wallet_emoji_id"
        const val publicKeyHexString = "wallet_public_key_hex_string"
    }

    private val logFileName = "tari_wallet.log"

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
        return "$walletFilesDirPath/$logFileName"
    }

    /**
     * Creates a new private key & stores if it doesn't exist.
     */
    private fun getPrivateKeyHexString(sharedPrefsWrapper: SharedPrefsWrapper): HexString {
        var hexString = sharedPrefsWrapper.getPrivateKeyHexString()
        if (hexString == null) {
            val privateKeyFFI = FFIPrivateKey()
            hexString = privateKeyFFI.toString()
            sharedPrefsWrapper.setPrivateKeyHexString(hexString)
            privateKeyFFI.destroy()
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
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String
    ): FFICommsConfig {
        //TODO: Change to tor
        val transport = FFITransportType()
        return FFICommsConfig(
            Constants.Wallet.WALLET_LISTENER_ADDRESS,
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
        @Named(FieldName.walletLogFilePath) logFilePath: String
    ): FFITestWallet {
        if (FFITestWallet.instance == null) {
            FFITestWallet.instance = FFITestWallet(commsConfig, logFilePath)
        }
        return FFITestWallet.instance!!
    }


    /**
     * Provides the emoji id of the wallet.
     */
    @Provides
    @Named(FieldName.emojiId)
    internal fun provideWalletEmojiId(wallet: FFITestWallet): String {
        val publicKeyFFI = wallet.getPublicKey()
        val emojiId = publicKeyFFI.getEmojiNodeId()
        publicKeyFFI.destroy()
        return emojiId
    }

    @Provides
    @Singleton
    @Named(FieldName.publicKeyHexString)
    internal fun provideWalletPublicKeyHexString(wallet: FFITestWallet): String {
        val publicKeyFFI = wallet.getPublicKey()
        val hexString = publicKeyFFI.toString()
        publicKeyFFI.destroy()
        return hexString
    }

}