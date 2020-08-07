package com.tari.android.wallet.infrastructure.log

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class LogsCleanupWorkerTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val testDriver: TestDriver
        get() = WorkManagerTestInitHelper.getTestDriver(context)!!

    private val workManager
        get() = WorkManager.getInstance(context)

    private val tempLogsDirectory: File
        get() = File(context.filesDir.absolutePath, "test_tari_logs")

    @Before
    fun setUp() {
        tempLogsDirectory.mkdir()
        val config: Configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        tempLogsDirectory.deleteRecursively()
    }

    @Test
    fun doWork_assertThatIllegalArgumentExceptionWasThrown_ifMaxSpaceIsNotProvided() {
        val input: Data = Data.Builder()
            .putString(LogsCleanupWorker.KEY_LOGS_PATH, tempLogsDirectory.absolutePath)
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.FAILED))
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_CAUSE),
            `is`(IllegalArgumentException::class.simpleName)
        )
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_MESSAGE),
            containsString("space")
        )
    }

    @Test
    fun doWork_assertThatIllegalArgumentExceptionWasThrown_ifLogsPathIsNotProvided() {
        val input: Data = Data.Builder()
            .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, 1024L)
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.FAILED))
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_CAUSE),
            `is`(IllegalArgumentException::class.simpleName)
        )
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_MESSAGE),
            containsString("path")
        )
    }

    @Test
    fun doWork_assertThatIllegalArgumentExceptionWasThrown_ifNothingExistsByTheProvidedLogsPath() {
        val input: Data = Data.Builder()
            .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, 1024L)
            .putString(LogsCleanupWorker.KEY_LOGS_PATH, "whatever")
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.FAILED))
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_CAUSE),
            `is`(IllegalArgumentException::class.simpleName)
        )
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_MESSAGE),
            containsString("exist")
        )
    }

    @Test
    fun doWork_assertThatIllegalArgumentExceptionWasThrown_ifProvidedPathIsActuallyAFile() {
        tempLogsDirectory.deleteRecursively()
        tempLogsDirectory.createNewFile()
        val input: Data = Data.Builder()
            .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, 1024L)
            .putString(LogsCleanupWorker.KEY_LOGS_PATH, tempLogsDirectory.absolutePath)
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.FAILED))
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_CAUSE),
            `is`(IllegalArgumentException::class.simpleName)
        )
        assertThat(
            workInfo.outputData.getString(LogsCleanupWorker.KEY_FAILURE_MESSAGE),
            containsString("file")
        )
    }

    @Test
    fun doWork_assertThatExcessiveFilesWereDeleted_ifValidPathAndMaxSpaceArgumentsWereProvided() {
        val firstFile = File(tempLogsDirectory, "log1.log")
        val secondFile = File(tempLogsDirectory, "log2.log")
        arrayOf(firstFile, secondFile).withIndex().forEach { (index, file) ->
            file.createNewFile()
            file.writeBytes(ByteArray(50))
            file.setLastModified((index + 1) * 1000L)
        }
        val input: Data = Data.Builder()
            .putString(LogsCleanupWorker.KEY_LOGS_PATH, tempLogsDirectory.absolutePath)
            .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, 70L)
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
        assertFalse(firstFile.exists())
    }

    @Test
    fun doWork_assertThatLastFileWasCleared_ifValidPathAndMaxSpaceArgumentsWereProvided_andMaxSpaceIsLesserThanTheOnlyFileLength() {
        val firstFile = File(tempLogsDirectory, "log1.log").apply {
            createNewFile()
            writeBytes(ByteArray(50))
            setLastModified(1000L)
        }
        val input: Data = Data.Builder()
            .putString(LogsCleanupWorker.KEY_LOGS_PATH, tempLogsDirectory.absolutePath)
            .putLong(LogsCleanupWorker.KEY_MAX_LOGS_SPACE, 30L)
            .build()
        val request = OneTimeWorkRequest.Builder(LogsCleanupWorker::class.java)
            .setInputData(input)
            .setInitialDelay(1L, TimeUnit.DAYS)
            .build()
        workManager.enqueue(request).result.get()
        testDriver.setInitialDelayMet(request.id)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.state, `is`(WorkInfo.State.SUCCEEDED))
        assertTrue(firstFile.exists())
        assertThat(firstFile.length(), `is`(0L))
    }

}
