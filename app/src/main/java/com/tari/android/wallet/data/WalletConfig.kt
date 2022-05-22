package com.tari.android.wallet.data

import android.content.Context
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import java.io.File

/**
 * Dagger module to inject wallet-related dependencies.
 *
 * @author The Tari Development Team
 */

class WalletConfig(val context: Context, val networkRepository: NetworkRepository){
    val walletDBName: String = "tari_wallet"
    val walletDBFullFileName: String = "$walletDBName.sqlite3"

    private val logFilePrefix = "tari_aurora"
    private val logFileExtension = "log"
    private val logFilesDirName = "tari_logs"

    /**
     * The directory in which the wallet files reside.
     */
    fun getWalletFilesDirPath(): String = context.filesDir.absolutePath + "/" + networkRepository.currentNetwork!!.network.uriComponent
    val walletDatabaseFilePath: String = File(getWalletFilesDirPath(), walletDBFullFileName).absolutePath

    /**
     * The directory in which the wallet log files reside.
     */
    fun getWalletLogFilesDirPath(): String {
        val logFilesDir = File(getWalletFilesDirPath(), logFilesDirName)
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
    fun getWalletLogFilePath(): String {
        val logFileName = "$logFilePrefix.$logFileExtension"
        val logFile = File(getWalletLogFilesDirPath(), logFileName)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        return logFile.absolutePath
    }

    fun getWalletTempDirPath() : String {
        val tempDir = File(getWalletFilesDirPath(), "temp")
        if (!tempDir.exists()) tempDir.mkdir()
        return tempDir.absolutePath
    }
}