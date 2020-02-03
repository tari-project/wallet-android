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
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.util.Constants
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject wallet-related dependencies.
 *
 * @author The Tari Development Team
 */
@Module
class WalletModule {

    companion object {
        const val NAME_WALLET_FILES_DIR_PATH = "WALLET_FILES_DIR_PATH"
        const val NAME_WALLET_LOG_FILE_PATH = "WALLET_LOG_FILE_PATH"
        private const val LOG_FILE_NAME = "tari_wallet.log"
        private const val PRIVATE_KEY_HEX_STRING: String =
            "6259C39F75E27140A652A5EE8AEFB3CF6C1686EF21D27793338D899380E8C801"
    }

    @Provides
    @Named(NAME_WALLET_FILES_DIR_PATH)
    internal fun provideWalletFilesDirPath(context: Context): String = context.filesDir.absolutePath

    @Provides
    @Named(NAME_WALLET_LOG_FILE_PATH)
    internal fun provideWalletLogFilePath(@Named(NAME_WALLET_FILES_DIR_PATH) walletFilesDirPath: String): String {
        val logFilePath = "$walletFilesDirPath/$LOG_FILE_NAME"

        // print last log
        var log = ""
        val logFile = File(logFilePath)
        if (logFile.exists()) {
            File(logFilePath).forEachLine {
                log += "\n" + it
            }
            Logger.d("FFI log file contents:\n$log")
        }

        return logFilePath
    }


    /**
     * Provides CommsConfig object for wallet configuration.
     */
    @Provides
    @Singleton
    internal fun provideCommsConfig(@Named(NAME_WALLET_FILES_DIR_PATH) path: String): FFICommsConfig {
        clearWalletFiles(path)
        return FFICommsConfig(
            Constants.Wallet.WALLET_CONTROL_SERVICE_ADDRESS,
            Constants.Wallet.WALLET_LISTENER_ADDRESS,
            Constants.Wallet.WALLET_DB_NAME,
            path,
            FFIPrivateKey(HexString(PRIVATE_KEY_HEX_STRING))
        )
    }

    /**
     * Provides wallet object.
     */
    @Provides
    @Singleton
    internal fun provideTestWallet(
        commsConfig: FFICommsConfig,
        @Named(NAME_WALLET_LOG_FILE_PATH) logFilePath: String
    ): FFITestWallet {
        return FFITestWallet(commsConfig, logFilePath)
    }

    /**
     * Utility function to clear all previous wallet files.
     */
    private fun clearWalletFiles(path: String): Boolean {
        val fileDirectory = File(path)
        val del = fileDirectory.deleteRecursively()
        if (!del) {
            return false
        }
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (directory.exists() && directory.canWrite() && directory.isDirectory) {
            return true
        }
        return false
    }

}