package com.tari.android.wallet.infrastructure.backup.compress

import com.tari.android.wallet.util.extension.getLastPathComponent
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipCompression : CompressionMethod {

    private const val READ_BYTE_BUFFER_SIZE = 2048
    override val extension: String = "zip"
    override val mimeType: String = "application/zip"

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
        BufferedInputStream(inputStreamProvider(), READ_BYTE_BUFFER_SIZE).use { origin ->
            zipEntryProvider().apply {
                time = sourceFile.lastModified()
                output.putNextEntry(this)
            }
            var count: Int
            val data = ByteArray(READ_BYTE_BUFFER_SIZE)
            while (origin.read(data, 0, READ_BYTE_BUFFER_SIZE).also { count = it } != -1) {
                output.write(data, 0, count)
            }
        }
    }

    override fun uncompress(source: File, destinationDirectory: File) {
        require(destinationDirectory.isDirectory)
        if (!destinationDirectory.exists()) destinationDirectory.mkdirs()
        val buffer = ByteArray(READ_BYTE_BUFFER_SIZE)
        ZipInputStream(BufferedInputStream(FileInputStream(source), READ_BYTE_BUFFER_SIZE)).use { zis ->
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