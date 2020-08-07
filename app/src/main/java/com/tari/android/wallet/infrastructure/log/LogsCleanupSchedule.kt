package com.tari.android.wallet.infrastructure.log

import androidx.work.*
import java.util.concurrent.TimeUnit

class LogsCleanupSchedule(
    private val manager: WorkManager,
    private val maxSpace: Long,
    private val logsPath: String
) {

    fun runDaily() {
        val work =
            PeriodicWorkRequest.Builder(LogsCleanupWorker::class.java, 1L, TimeUnit.DAYS)
                .setInitialDelay(1L, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .setInputData(
                    Data.Builder()
                        .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, maxSpace)
                        .putString(LogsCleanupWorker.KEY_LOGS_PATH, logsPath)
                        .build()
                )
                .build()
        manager.enqueueUniquePeriodicWork(CLEANUP_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work)
    }

    private companion object {
        private const val CLEANUP_WORK_NAME = "logs_cleanup_worker"
    }

}
