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
package com.tari.android.wallet.extension

import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * File extensions - currently for backup and restore functions.
 *
 * @author The Tari Development Team
 */

val File.extension: String
    get() = name.substringAfterLast('.', "")

fun File.getLastPathComponent(): String {
    val segments = absolutePath.split("/".toRegex()).toTypedArray()
    return if (segments.isEmpty()) "" else segments[segments.size - 1]
}

fun List<File>.compress(
    compressionMethod: CompressionMethod,
    targetFilePath: String
): File {
    compressionMethod.compress(
        targetFilePath,
        this
    )
    return File(targetFilePath)
}

fun File.encrypt(
    encryptionAlgorithm: SymmetricEncryptionAlgorithm,
    key: CharArray,
    targetFilePath: String
): File {
    val targetFile = File(targetFilePath)
    encryptionAlgorithm.encrypt(
        key,
        { FileInputStream(this) },
        { FileOutputStream(targetFile) }
    )
    return targetFile
}