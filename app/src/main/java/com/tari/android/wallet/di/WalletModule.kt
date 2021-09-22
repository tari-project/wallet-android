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
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.network.NetworkConnectionStateReceiver
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
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
internal class WalletModule {

    object FieldName {
        const val walletFilesDirPath = "wallet_files_dir_path"
        const val walletLogFilesDirPath = "wallet_log_file_dir_path"
        const val walletLogFilePath = "wallet_log_file_path"
        const val walletDatabaseFilePath = "wallet_database_file_path"
        const val walletTempDirPath = "wallet_temp_dir_path"
    }

    private val logFilePrefix = "tari_aurora"
    private val logFileExtension = "log"
    private val logFilesDirName = "tari_logs"

    /**
     * The directory in which the wallet files reside.
     */
    @Provides
    @Named(FieldName.walletFilesDirPath)
    @Singleton
    fun provideWalletFilesDirPath(context: Context): String = context.filesDir.absolutePath

    /**
     * The directory in which the wallet log files reside.
     */
    @Provides
    @Named(FieldName.walletLogFilesDirPath)
    @Singleton
    fun provideLogFilesDirPath(
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String
    ): String {
        val logFilesDir = File(walletFilesDirPath, logFilesDirName)
        if (!logFilesDir.exists()) {
            logFilesDir.mkdir()
        } else { // delete older log files
            val files = logFilesDir.listFiles()?.filter { !it.name.contains(logFilePrefix) }
            files?.forEach { it.delete() }
        }
        return logFilesDir.absolutePath
    }

    /**
     * FFI log file path.
     */
    @Provides
    @Named(FieldName.walletLogFilePath)
    @Singleton
    fun provideWalletLogFilePath(@Named(FieldName.walletLogFilesDirPath) logFilesDirPath: String): String {
        val logFileName = "$logFilePrefix.$logFileExtension"
        val logFile = File(logFilesDirPath, logFileName)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        return logFile.absolutePath
    }

    @Provides
    @Named(FieldName.walletDatabaseFilePath)
    @Singleton
    fun provideWalletDatabaseFilePath(
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String
    ): String {
        return File(walletFilesDirPath, Constants.Wallet.walletDBFullFileName).absolutePath
    }

    @Provides
    @Named(FieldName.walletTempDirPath)
    @Singleton
    fun provideWalletTempDirPath(
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String
    ): String {
        val tempDir = File(walletFilesDirPath, "temp")
        if (!tempDir.exists()) tempDir.mkdir()
        return tempDir.absolutePath
    }

    @Provides
    @Singleton
    fun provideWalletManager(
        context: Context,
        @Named(FieldName.walletFilesDirPath) walletFilesDirPath: String,
        @Named(FieldName.walletLogFilePath) walletLogFilePath: String,
        torConfig: TorConfig,
        torProxyManager: TorProxyManager,
        sharedPrefsWrapper: SharedPrefsRepository,
        baseNodeSharedRepository: BaseNodeSharedRepository,
        seedPhraseRepository: SeedPhraseRepository,
        baseNodes: BaseNodes
    ): WalletManager = WalletManager(
        context,
        walletFilesDirPath,
        walletLogFilePath,
        torProxyManager,
        sharedPrefsWrapper,
        baseNodeSharedRepository,
        seedPhraseRepository,
        baseNodes,
        torConfig
    )

    @Provides
    @Singleton
    fun provideNetworkConnectionStatusReceiver(): NetworkConnectionStateReceiver {
        return NetworkConnectionStateReceiver()
    }

    @Provides
    @Singleton
    fun provideBugReportingService(
        sharedPrefsWrapper: SharedPrefsRepository,
        @Named(FieldName.walletLogFilesDirPath) logFilesDirPath: String
    ): BugReportingService {
        return BugReportingService(sharedPrefsWrapper, logFilesDirPath)
    }

    @Provides
    @Singleton
    fun provideSeedPhraseRepository() = SeedPhraseRepository()

    @Provides
    @Singleton
    fun provideBaseNodes(context: Context, baseNodeSharedRepository: BaseNodeSharedRepository) = BaseNodes(context, baseNodeSharedRepository)
}
