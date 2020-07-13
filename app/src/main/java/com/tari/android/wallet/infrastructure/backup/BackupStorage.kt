package com.tari.android.wallet.infrastructure.backup

import android.content.Intent
import androidx.fragment.app.Fragment
import org.joda.time.DateTime

interface BackupStorage {

    fun setup(hostFragment: Fragment)

    suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?)

    suspend fun backup(newPassword: CharArray? = null): DateTime

    suspend fun hasBackupForDate(date: DateTime): Boolean

    suspend fun restoreLatestBackup(password: String? = null)

    suspend fun signOut()

    suspend fun deleteAllBackupFiles()

}