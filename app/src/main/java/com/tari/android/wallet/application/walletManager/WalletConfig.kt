package com.tari.android.wallet.application.walletManager

import android.content.Context
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the file directory structure for the Tari Android Wallet.
 *
 * The file structure is organized as follows:
 *
 * com.tari.android.wallet
 * └── files/
 *     └── {network}/
 *         ├── temp/                    // Temporary files specific to the network.
 *         ├── temp_wallet/             // Temporary wallet-related files. (Paper wallet files)
 *         ├── tari_logs/               // Directory for Tari log files.
 *         │   ├── tari_aurora{n}.log   // Log files, where {n} is the log sequence number.
 *         └── tari_wallet.sqlite3      // SQLite database file for the Tari wallet.
 *
 * The `{network}` directory refers to the specific network being used, such as `mainnet` or `testnet`.
 *
 * All the paper wallet files are stored in the temp_wallet directory. In future, we may support multiple wallets, but as for now, we support only
 * one wallet at a time, so the wallet files are stored in the main directory.
 */
@Singleton
class WalletConfig @Inject constructor(
    val context: Context,
    val networkRepository: NetworkPrefRepository,
) {

    /**
     * The directory in which the wallet files reside.
     */
    fun getWalletFilesDirPath(isMainWallet: Boolean = true): String {
        val baseDir = File(context.filesDir, networkRepository.currentNetwork.network.uriComponent)
        val walletDir = if (isMainWallet) {
            File(baseDir, MAIN_WALLET_DB_DIR)
        } else {
            File(baseDir, TEMP_WALLET_DB_DIR)
        }
        if (!walletDir.exists()) walletDir.mkdir()
        return walletDir.absolutePath
    }

    fun getWalletDatabaseFilePath(main: Boolean = true): String = File(getWalletFilesDirPath(main), WALLET_DB_FULL_FILE_NAME).absolutePath

    /**
     * The directory in which the wallet log files reside.
     */
    fun getWalletLogFilesDirPath(): String {
        val logFilesDir = File(getWalletFilesDirPath(), LOG_FILES_DIR_NAME)
        if (!logFilesDir.exists()) {
            logFilesDir.mkdir()
        } else { // delete older log files
            val files = logFilesDir.listFiles()?.filter { !it.name.contains(LOG_FILE_PREFIX) }
            files?.forEach { it.delete() }
        }
        return logFilesDir.absolutePath
    }

    fun getWalletLogFilePath(): String = getOrCreateFilePath(getWalletLogFilesDirPath(), "$LOG_FILE_PREFIX.$LOG_FILE_EXTENSION")

    private fun getOrCreateFilePath(dirPath: String, fileName: String): String {
        val folder = File(dirPath)
        if (!folder.exists()) folder.mkdirs()

        val file = File(dirPath, fileName)
        if (!file.exists()) file.createNewFile()
        return file.absolutePath
    }

    fun getWalletTempDirPath(): String {
        val tempDir = File(getWalletFilesDirPath(), "temp")
        if (!tempDir.exists()) tempDir.mkdir()
        return tempDir.absolutePath
    }

    /**
     * Utility function to clear all previous wallet files.
     */
    fun clearWalletFiles(isMainWallet: Boolean = true): Boolean {
        val path = getWalletFilesDirPath(isMainWallet)
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

    fun getLogFiles(): List<File> {
        val dirPath = getWalletLogFilesDirPath()
        val root = File(dirPath)
        if (!root.isDirectory) return Collections.emptyList()
        val files = root.listFiles()!!.toMutableList()
        val filteredFiles = files.filter { it.extension == "log" }.toMutableList()
        filteredFiles.sortBy { it.name }
        // actual log file will be at the end of the sorted list due to the log file rolling
        // naming convention, move it to the top
        if (filteredFiles.size > 1) {
            val currentLogFile = filteredFiles.removeAt(filteredFiles.size - 1)
            filteredFiles.add(0, currentLogFile)
        }
        return filteredFiles
    }

    fun walletExists(isMainWallet: Boolean = true): Boolean = File(getWalletFilesDirPath(isMainWallet), WALLET_DB_FULL_FILE_NAME).exists()

    companion object {
        const val WALLET_DB_NAME = "tari_wallet"
        private const val LOG_FILE_PREFIX = "tari_aurora"
        private const val LOG_FILE_EXTENSION = "log"
        private const val LOG_FILES_DIR_NAME = "tari_logs"
        private const val WALLET_DB_FULL_FILE_NAME = "$WALLET_DB_NAME.sqlite3"
        private const val MAIN_WALLET_DB_DIR = "" // Empty for the main wallet directory
        private const val TEMP_WALLET_DB_DIR = "temp_wallet_db"

        val balanceFormatter = DecimalFormat("#,##0.00").apply {
            roundingMode = RoundingMode.FLOOR
        }

        val amountFormatter = DecimalFormat("#,##0.00####").apply {
            roundingMode = RoundingMode.FLOOR
        }
    }
}