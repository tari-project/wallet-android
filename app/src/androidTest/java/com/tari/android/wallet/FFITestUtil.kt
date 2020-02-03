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
package com.tari.android.wallet

import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.NetAddressString
import java.io.File

/**
 * Test utilities.
 *
 * @author The Tari Development Team
 */
class FFITestUtil {

    companion object {

        private const val WALLET_LOG_FILE_NAME: String = "tari_log.txt"
        private val WALLET_FILES_DIR_PATH: String = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .filesDir
            .absolutePath
        internal const val WALLET_DB_NAME: String = "tari_test_db"
        internal val WALLET_CONTROL_SERVICE_ADDRESS: NetAddressString = NetAddressString("127.0.0.1", 80)
        internal val WALLET_LISTENER_ADDRESS: NetAddressString = NetAddressString("0.0.0.0", 0)
        internal val WALLET_DATASTORE_PATH: String = WALLET_FILES_DIR_PATH
        internal val WALLET_LOG_FILE_PATH: String = "$WALLET_FILES_DIR_PATH/$WALLET_LOG_FILE_NAME"

        // Matching pair of keys.
        internal const val PUBLIC_KEY_HEX_STRING: String =
            "30E1DFA197794858BFDBF96CDCE5DC8637D4BD1202DC694991040DDECBF42D40"
        internal const val PRIVATE_KEY_HEX_STRING: String =
            "6259C39F75E27140A652A5EE8AEFB3CF6C1686EF21D27793338D899380E8C801"

        fun generateRandomAlphanumericString(len: Int): String {
            val characters = ('0'..'z').toList().toTypedArray()
            return (1..len).map { characters.random() }.joinToString("")
        }

        fun clearTestFiles(path: String): Boolean {
            val fileDirectory = File(path)
            val del = fileDirectory.deleteRecursively()
            if (!del) {
                return false
            }
            val directory = File(path)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            if (directory.exists() && directory.canWrite() && directory.isDirectory) {
                return true
            }
            return false
        }

        fun printFFILogFile() {
            var log = ""
            File(WALLET_LOG_FILE_PATH).forEachLine {
                log += "\n" + it
            }
            Logger.d("FFI log file contents:\n$log")
        }

    }

}