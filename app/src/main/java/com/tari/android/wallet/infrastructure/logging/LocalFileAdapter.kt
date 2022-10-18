package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class LocalFileAdapter(private val filePath: String) : LogAdapter {

    override fun isLoggable(priority: Int, tag: String?): Boolean = true

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun log(priority: Int, tag: String?, message: String) {
        runCatching {
            BufferedWriter(OutputStreamWriter(FileOutputStream(File(filePath), true))).use {
                val priorityName = when (priority) {
                    2 -> Logger::VERBOSE.name
                    3 -> Logger::DEBUG.name
                    4 -> Logger::INFO.name
                    5 -> Logger::WARN.name
                    6 -> Logger::ERROR.name
                    else -> Logger::ASSERT.name
                }
                val dateTimeNow = dateTimeFormatter.format(LocalDateTime.now())
                val debugLine = "$dateTimeNow [${tag ?: ""}] $priorityName ${message.replace("\n", " ")}"
                it.appendLine(debugLine)
            }
        }
    }
}