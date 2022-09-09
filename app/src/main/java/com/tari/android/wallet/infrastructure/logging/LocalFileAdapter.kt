package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import org.joda.time.DateTime
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class LocalFileAdapter(private val filePath: String) : LogAdapter {

    override fun isLoggable(priority: Int, tag: String?): Boolean = true

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
                val dateTimeNow = DateTime.now().toString()
                it.appendLine("$priorityName | $dateTimeNow | ${tag ?: ""} | ${message.replace("\n", " ")}")
            }
        }
    }
}