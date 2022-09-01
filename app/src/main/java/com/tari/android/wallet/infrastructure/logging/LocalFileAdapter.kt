package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import java.io.BufferedWriter
import java.io.File

class LocalFileAdapter(filePath: String) : LogAdapter {

    private val bufferedWriter = BufferedWriter(File(filePath).writer())

    override fun isLoggable(priority: Int, tag: String?): Boolean = true

    override fun log(priority: Int, tag: String?, message: String) {
        val priorityName = when (priority) {
            2 -> Logger::VERBOSE.name
            3 -> Logger::DEBUG.name
            4 -> Logger::INFO.name
            5 -> Logger::WARN.name
            6 -> Logger::ERROR.name
            else -> Logger::ASSERT.name
        }
        bufferedWriter.write("$priorityName ${tag ?: ""}: ${message.replace("\n", " ")}")
        bufferedWriter.newLine()
        bufferedWriter.flush()
    }
}