package com.tari.android.wallet.extension

import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val File.extension: String
    get() = name.substringAfterLast('.', "")

fun File.getLastPathComponent(): String? {
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