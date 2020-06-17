package com.tari.android.wallet.di

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.backup.WalletBackup
import com.tari.android.wallet.infrastructure.backup.WalletRestorationFactory
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorageFactory
import com.tari.android.wallet.ui.extension.string
import dagger.Module
import dagger.Provides
import java.io.File

@Module
class BackupAndRestoreModule {

    @Provides
    @BackupAndRestoreScope
    fun provideWalletBackup(workingDir: File) = WalletBackup.defaultStrategy(workingDir)

    @Provides
    @BackupAndRestoreScope
    fun provideBackupStorageFactory(context: Context) =
        BackupStorageFactory(context.string(R.string.app_name))

    @Provides
    @BackupAndRestoreScope
    fun provideRestorationFactory(workingDir: File) =
        WalletRestorationFactory.defaultStrategy(workingDir)

}
