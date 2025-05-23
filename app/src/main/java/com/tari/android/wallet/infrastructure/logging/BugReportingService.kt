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
package com.tari.android.wallet.infrastructure.logging

import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import io.sentry.Attachment
import io.sentry.Sentry
import io.sentry.UserFeedback
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportingService @Inject constructor(
    private val sharedPrefsWrapper: CorePrefRepository,
    private val walletConfig: WalletConfig,
) {

    fun share(name: String, email: String, bugDescription: String) {
        val zippedFile = getZippedLogs()
        val eventId = Sentry.captureMessage(bugDescription) { scope ->
            scope.addAttachment(Attachment(zippedFile.absolutePath))
        }
        val sentryUserFeedback = UserFeedback(eventId, name, email, bugDescription)
        Sentry.captureUserFeedback(sentryUserFeedback)
    }

    private fun getZippedLogs(): File {
        val walletAddress = sharedPrefsWrapper.walletAddressBase58

        val logFilesDirPath = walletConfig.getWalletLogFilesDirPath()
        val zipFile = File(logFilesDirPath, "ffi_logs_${walletAddress}.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        val allLogFiles = walletConfig.getLogFiles()

        val fileOutStream: FileOutputStream
        val bufferOutStream: BufferedOutputStream
        val out: ZipOutputStream

        try {
            fileOutStream = FileOutputStream(zipFile)
            bufferOutStream = BufferedOutputStream(fileOutStream)
            out = ZipOutputStream(bufferOutStream)

            try {
                for (file in allLogFiles) {
                    val inputStream: FileInputStream
                    val origin: BufferedInputStream

                    try {
                        inputStream = FileInputStream(file)
                        origin = BufferedInputStream(inputStream)

                        try {
                            val entry = ZipEntry(file.name)
                            out.putNextEntry(entry)
                            origin.copyTo(out, 1024)
                        } finally {
                            origin.close()
                            inputStream.close()
                        }
                    } catch (e: IOException) {
                        error("Error reading or processing file: ${file.name}")
                    }
                }
            } finally {
                out.closeEntry()
                out.close()
                bufferOutStream.close()
                fileOutStream.close()
            }
        } catch (e: FileNotFoundException) {
            error("Output zip file not found: ${zipFile.absolutePath}")
        } catch (e: IOException) {
            error("Error creating zip file")
        }
        return zipFile.takeIf { it.exists() } ?: error("Zipped logs file is null or does not exist")
    }
}