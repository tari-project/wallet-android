/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ffi

import android.os.FileObserver
import android.util.Log
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.File
import java.io.FileReader
import java.io.LineNumberReader
import java.nio.charset.StandardCharsets

/**
 * Observes the log file and on a file modify, forwards the new line (if any) in
 * the log file to the Android log.
 *
 * @author The Tari Development Team
 */
@Suppress("DEPRECATION")
internal class LogFileObserver(logFilePath: String) : FileObserver(logFilePath) {

    companion object {
        var instance: LogFileObserver? = null
    }

    private val logTag = "FFI"
    private val logFile = File(logFilePath)

    /**
     * Remember the last number of lines to be able to log any new lines.
     */
    private var lastNumberOfLines = 0

    private fun logNewLines() {
        // read number of lines
        val fileReader = FileReader(logFile)
        val lineNumberReader = LineNumberReader(fileReader)
        lineNumberReader.skip(Long.MAX_VALUE)
        // figured out how many new lines there are
        val numberOfNewLines = (lineNumberReader.lineNumber) - lastNumberOfLines
        // read last new lines
        val reversedFileReader = ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)
        val lineList = mutableListOf<String>()
        for (i in 0 until numberOfNewLines) {
            val line = reversedFileReader.readLine()
            lineList.add(line)
        }
        // log them in reverse order
        lineList.reversed().forEach { logLine ->
            Log.d(logTag, logLine)
        }
        lastNumberOfLines = lineNumberReader.lineNumber
        // close resources
        fileReader.close()
        lineNumberReader.close()
        reversedFileReader.close()
    }

    override fun onEvent(event: Int, path: String?) {
        logNewLines()
    }

}