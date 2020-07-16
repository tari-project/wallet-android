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
package com.tari.android.wallet.infrastructure.backup.storage

import com.tari.android.wallet.infrastructure.backup.BackupNamingPolicy
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.tz.Provider
import org.joda.time.tz.UTCProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TariBackupNameValidationPolicyTest {

    @get:Rule
    val rule = JodaAndroidFixRule()

    private val policy = BackupNamingPolicy

    @Test
    fun `assert expected valid date inside archive name with zip extension`() {
        val name = "Tari-Aurora-Backup-2020-05-25_12-00-00.zip"
        val expected = DateTime(2020, 5, 25, 12, 0, 0, DateTimeZone.UTC)
        assertEquals(expected, policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected valid date inside archive name with rar extension`() {
        val name = "Tari-Aurora-Backup-2020-05-25_12-00-00.rar"
        val expected = DateTime(2020, 5, 25, 12, 0, 0, DateTimeZone.UTC)
        assertEquals(expected, policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected null date if month is 0`() {
        val name = "Tari-Aurora-Backup-2020-00-25_12-00-00.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected null date if day is 0`() {
        val name = "Tari-Aurora-Backup-2020-01-00_12-00-00.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected null date if date part is invalid`() {
        val name = "Tari-Aurora-Backup-2020-01-0_12-00-00.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected date if hour is 0`() {
        val name = "Tari-Aurora-Backup-2020-01-01_00-05-05.zip"
        assertEquals(
            DateTime(2020, 1, 1, 0, 5, 5, DateTimeZone.UTC),
            policy.getDateFromBackupFileName(name)
        )
    }

    @Test
    fun `assert expected null date if hour is 24`() {
        val name = "Tari-Aurora-Backup-2020-01-01_24-00-00.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected date if minute is 0`() {
        val name = "Tari-Aurora-Backup-2020-01-01_05-00-05.zip"
        assertEquals(
            DateTime(2020, 1, 1, 5, 0, 5, DateTimeZone.UTC),
            policy.getDateFromBackupFileName(name)
        )
    }

    @Test
    fun `assert expected null date if minute is 60`() {
        val name = "Tari-Aurora-Backup-2020-01-01_05-60-05.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

    @Test
    fun `assert expected date if second is 0`() {
        val name = "Tari-Aurora-Backup-2020-01-01_05-05-00.zip"
        assertEquals(
            DateTime(2020, 1, 1, 5, 5, 0, DateTimeZone.UTC),
            policy.getDateFromBackupFileName(name)
        )
    }

    @Test
    fun `assert expected null date if second is 60`() {
        val name = "Tari-Aurora-Backup-2020-01-01_05-00-60.zip"
        assertNull(policy.getDateFromBackupFileName(name))
    }

}

class JodaAndroidFixRule @JvmOverloads constructor(private val provider: Provider = UTCProvider()) :
    TestRule {
    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                DateTimeZone.setProvider(provider)
                base.evaluate()
            }
        }
    }

}
