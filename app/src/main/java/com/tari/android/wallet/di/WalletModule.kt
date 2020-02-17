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
import dagger.Module
import dagger.Provides
import java.lang.RuntimeException
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
    }

    private val logFileName = "tari_wallet.log"
    private val privateKeyHexString: String =
        "6259C39F75E27140A652A5EE8AEFB3CF6C1686EF21D27793338D899380E8C801"

    @Provides
    @Named(FieldName.walletFilesDirPath)
    internal fun provideWalletFilesDirPath(context: Context): String = context.filesDir.absolutePath

    @Provides
    @Named(FieldName.walletLogFilePath)
    internal fun provideWalletLogFilePath(@Named(FieldName.walletFilesDirPath) walletFilesDirPath: String): String {
        return "$walletFilesDirPath/$logFileName"
    }

    /**
     * Provides CommsConfig object for wallet configuration.
     */
    @Provides
    @Singleton
    internal fun provideCommsConfig(
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String
    ): FFICommsConfig {
        return FFICommsConfig(
            Constants.Wallet.WALLET_CONTROL_SERVICE_ADDRESS,
            Constants.Wallet.WALLET_LISTENER_ADDRESS,
            Constants.Wallet.WALLET_DB_NAME,
            walletFilesDirPath,
            FFIPrivateKey(HexString(privateKeyHexString))
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
    internal fun provideWalletEmojiId(): String {
        val wallet = FFITestWallet.instance
            ?: throw RuntimeException("Wallet has not been initialized yet.")
        return wallet.getPublicKey().getEmoji()
    }

}