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
package com.tari.android.wallet.infrastructure.backup

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

internal object BackupNamingPolicy {

    private const val backupFileNamePrefix = "Tari-Aurora-Backup-"

    // Tari-Aurora-Backup_yyyy-MM-dd_HH-mm-ss.*
    private val regex =
        Regex("Tari-Aurora-Backup-(\\d{4}-(((0)[1-9])|((1)[0-2]))-((0)[1-9]|[1-2][0-9]|(3)[0-1])_([0-1][0-9]|(2)[0-3])-([0-5][0-9])-([0-5][0-9]))\\..+")
    private val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd_HH-mm-ss")

    fun getBackupFileName(backupDate: DateTime = DateTime.now()): String {
        return backupFileNamePrefix + dateFormatter.print(backupDate)
    }

    fun isBackupFileName(fileName: String): Boolean {
        return regex.matches(fileName)
    }

    fun getDateFromBackupFileName(name: String): DateTime? =
        regex.find(name)?.let { it.groups[1]!!.value }?.let { dateFormatter.parseDateTime(it) }

}