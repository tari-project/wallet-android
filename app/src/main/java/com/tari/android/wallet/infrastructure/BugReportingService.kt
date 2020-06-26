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
package com.tari.android.wallet.infrastructure

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.tari.android.wallet.R
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Contains one public function that zips the last numberOfLogsFilesToShare log files,
 * zips them and shares the zip file through an action intent.
 *
 * @author The Tari Development Team
 */
internal class BugReportingService(private val sharedPrefsWrapper: SharedPrefsWrapper,
                                   private val logFilesDirPath: String) {

    class BugReportFileSizeLimitExceededException: Exception()

    private val numberOfLogsFilesToShare = 2
    private val maxLogZipFileSizeBytes = 25 * 1024 * 1024

    fun shareBugReport(context: Context) {
        // delete if zipped file exists
        val publicKeyHex = sharedPrefsWrapper.publicKeyHexString
        val zipFile = File(
            logFilesDirPath,
            "ffi_logs_${publicKeyHex}.zip"
        )
        if (zipFile.exists()) {
            zipFile.delete()
        }
        val fileOutStream = FileOutputStream(zipFile)
        // zip!
        val allLogFiles = WalletUtil.getLogFilesFromDirectory(logFilesDirPath)
        val logFilesToShare = allLogFiles.take(numberOfLogsFilesToShare)
        ZipOutputStream(BufferedOutputStream(fileOutStream)).use { out ->
            for (file in logFilesToShare) {
                FileInputStream(file).use { inputStream ->
                    BufferedInputStream(inputStream).use { origin ->
                        val entry = ZipEntry(file.name)
                        out.putNextEntry(entry)
                        origin.copyTo(out, 1024)
                        origin.close()
                    }
                    inputStream.close()
                }
            }
            out.closeEntry()
            out.close()
        }
        // check zip file size
        if (zipFile.length() > maxLogZipFileSizeBytes) {
            zipFile.delete()
            throw BugReportFileSizeLimitExceededException()
        } else {
            shareLogZipFile(context, zipFile)
        }
    }

    private fun shareLogZipFile(context: Context, zipFile: File) {
        // file is zipped, create the intent
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        val intent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        val zipFileUri = FileProvider.getUriForFile(
            context,
            "com.tari.android.wallet.files",
            zipFile
        )
        intent.putExtra(Intent.EXTRA_STREAM, zipFileUri)
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Public Key:\n" + sharedPrefsWrapper.publicKeyHexString + "\n\n"
                    + "Emoji Id:\n" + sharedPrefsWrapper.emojiId
        )
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf(context.getString(R.string.ffi_admin_email_address))
        )
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            "Aurora Android Bug Report"
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.selector = emailIntent
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.common_share)
            )
        )
    }

}