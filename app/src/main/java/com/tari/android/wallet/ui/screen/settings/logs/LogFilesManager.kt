package com.tari.android.wallet.ui.screen.settings.logs

import com.tari.android.wallet.application.walletManager.WalletConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogFilesManager(private val walletConfig: WalletConfig) {

    suspend fun manage() = withContext(Dispatchers.IO) {
        val files = walletConfig.getLogFiles()
        if (files.size > MAX_FILES) {
            files.sortedByDescending { it.lastModified() }.drop(10).forEach { fileToDelete ->
                runCatching { fileToDelete.delete() }
            }
        }
    }

    companion object {
        const val MAX_FILES = 10
    }
}