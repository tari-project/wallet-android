package com.tari.android.wallet.infrastructure.log

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger
import java.io.File

class LogsCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    // DI for Workers is cumbersome and does not worth the effort, so parameters are used instead
    // https://proandroiddev.com/dagger-2-setup-with-workmanager-a-complete-step-by-step-guild-bb9f474bde37
    @ExperimentalStdlibApi
    override fun doWork(): Result = try {
        val space = inputData.getLong(KEY_MAX_LOGS_SPACE, -1L)
        require(space >= 0) { "Max logs space was not specified in the inputData" }
        val logsPath = inputData.getString(KEY_LOGS_PATH)
            ?: throw IllegalArgumentException("Logs path was not specified in the inputData")
        val logsDirectory = File(logsPath)
        require(logsDirectory.exists()) { "Logs directory does not exist" }
        require(logsDirectory.isDirectory) { "Provided logs directory path is actually a file: ${logsDirectory.absolutePath}" }
        FileTree.fromDir(logsDirectory).shrink(space) { it.writeBytes(ByteArray(0)) }
        Result.success()
    } catch (e: Exception) {
        Logger.e(e, "Exception caught in LogsCleanupWorker's doWork()")
        Result.failure(
            Data.Builder()
                .putString(KEY_FAILURE_CAUSE, e.javaClass.simpleName)
                .putString(KEY_FAILURE_MESSAGE, e.message)
                .build()
        )
    }

    companion object {
        const val KEY_MAX_LOGS_SPACE = "max_logs_space"
        const val KEY_LOGS_PATH = "logs_path"
        const val KEY_FAILURE_CAUSE = "failure_cause"
        const val KEY_FAILURE_MESSAGE = "failure_message"
    }

}
