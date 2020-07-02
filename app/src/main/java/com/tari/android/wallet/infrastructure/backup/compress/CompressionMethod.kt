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
package com.tari.android.wallet.infrastructure.backup.compress

import com.tari.android.wallet.extension.getLastPathComponent
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface CompressionMethod {

    val extension: String
    val mimeType: String

    fun compress(outputPath: String, files: Iterable<File>): File

    fun uncompress(source: File, destinationDirectory: File)

    companion object {
        fun zip(): CompressionMethod = ZipCompression
    }
}

private object ZipCompression : CompressionMethod {

    private const val READ_BYTE_BUFFER_SIZE = 2048
    override val extension: String
        get() = "zip"
    override val mimeType: String
        get() = "application/zip"

    override fun compress(outputPath: String, files: Iterable<File>): File {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use { destination ->
            files.forEach { file ->
                if (file.isDirectory) compressFolder(destination, file, file.parent!!.length)
                else compressFile(file, destination, { FileInputStream(file) }) {
                    ZipEntry(file.getLastPathComponent())
                }
            }
        }
        return File(outputPath)
    }

    private fun compressFolder(destination: ZipOutputStream, folder: File, pathLength: Int): Unit =
        (folder.listFiles() ?: emptyArray()).forEach { file ->
            if (file.isDirectory) compressFolder(destination, file, pathLength)
            else compressFile(file, destination, { FileInputStream(file.path) }) {
                ZipEntry(file.path.substring(pathLength))
            }
        }

    private fun compressFile(
        sourceFile: File,
        output: ZipOutputStream,
        inputStreamProvider: () -> FileInputStream,
        zipEntryProvider: () -> ZipEntry
    ) {
        val origin = BufferedInputStream(inputStreamProvider(), READ_BYTE_BUFFER_SIZE)
        val entry = zipEntryProvider()
        entry.time = sourceFile.lastModified()
        output.putNextEntry(entry)
        var count: Int
        val data = ByteArray(READ_BYTE_BUFFER_SIZE)
        while (origin.read(data, 0, READ_BYTE_BUFFER_SIZE).also { count = it } != -1) {
            output.write(data, 0, count)
        }
        origin.close()
    }

    override fun uncompress(source: File, destinationDirectory: File) {
        require(destinationDirectory.isDirectory)
        if (!destinationDirectory.exists()) destinationDirectory.mkdirs()
        val buffer = ByteArray(READ_BYTE_BUFFER_SIZE)
        ZipInputStream(
            BufferedInputStream(FileInputStream(source), READ_BYTE_BUFFER_SIZE)
        ).use { zis ->
            generateSequence { zis.nextEntry }
                .forEach { entry ->
                    val newFile = File(destinationDirectory, entry.name)
                    File(newFile.parent!!).mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                    zis.closeEntry()
                }
        }
    }

}
