package com.tari.android.wallet.di

import android.content.Context
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.infrastructure.backup.LocalBackupStorage
import com.tari.android.wallet.util.SharedPrefsWrapper
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class BackupAndRestoreModule {

    @Provides
    @Singleton
    fun provideBackupFileProcessor(
        sharedPrefs: SharedPrefsWrapper,
        @Named(WalletModule.FieldName.walletFilesDirPath) walletFilesDirPath: String,
        @Named(WalletModule.FieldName.walletDatabaseFilePath) walletDatabaseFilePath: String,
        @Named(WalletModule.FieldName.walletTempDirPath) walletTempDirPath: String
    ): BackupFileProcessor = BackupFileProcessor(
        sharedPrefs,
        walletFilesDirPath,
        walletDatabaseFilePath,
        walletTempDirPath
    )

    @Provides
    @Singleton
    fun provideBackupStorage(
        context: Context,
        sharedPrefs: SharedPrefsWrapper,
        @Named(WalletModule.FieldName.walletTempDirPath) walletTempDirPath: String,
        backupFileProcessor: BackupFileProcessor
    ): BackupStorage = LocalBackupStorage(
        context,
        sharedPrefs,
        walletTempDirPath,
        backupFileProcessor
    )

    @Provides
    @Singleton
    fun provideBackupManager(
        sharedPrefs: SharedPrefsWrapper,
        backupStorage: BackupStorage
    ): BackupManager = BackupManager(sharedPrefs, backupStorage)

}
