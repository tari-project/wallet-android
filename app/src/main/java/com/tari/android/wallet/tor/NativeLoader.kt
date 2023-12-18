package com.tari.android.wallet.tor

import android.os.Build
import com.orhanobut.logger.Logger
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

object NativeLoader {

    private val logger
        get() = Logger.t(LoggerTags.Connection.name)

    private fun loadFromZip(appSourceDir: File, libName: String, destLocalFile: File, arch: String): Boolean {

        var zipFile: ZipFile? = null
        var stream: InputStream? = null

        try {

            zipFile = ZipFile(appSourceDir)
            var entry = zipFile.getEntry("lib/$arch/$libName.so")

            if (entry == null) {
                entry = zipFile.getEntry("jni/$arch/$libName.so")

                if (entry == null)
                    throw Exception("Unable to find file in apk:lib/$arch/$libName")
            }
            //how we wrap this in another stream because the native .so is zipped itself
            stream = zipFile.getInputStream(entry)
            val out: OutputStream = FileOutputStream(destLocalFile)
            val buf = ByteArray(4096)
            var len: Int

            while (stream.read(buf).also { len = it } > 0) {
                Thread.yield()
                out.write(buf, 0, len)
            }

            out.close()
            destLocalFile.setReadable(true, true)
            destLocalFile.setExecutable(true, false)
            destLocalFile.setWritable(true)

            return true
        } catch (e: Exception) {
            logger.i(e.toString() + "Load from zip")
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: Exception) {
                    logger.i(e.toString() + "Closing stream")
                }
            }

            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (e: Exception) {
                    logger.i(e.toString() + "closing zip file")
                }
            }
        }
        return false
    }

    fun loadNativeBinary(appNativeDir: File, appSourceDir: File, libName: String, destLocalFile: File?): File? {

        try {
            val fileNativeBin = File(appNativeDir.path, "$libName.so")
            if (fileNativeBin.exists()) {
                if (fileNativeBin.canExecute())
                    return fileNativeBin
                else {
                    TorResourceInstaller.makeFileExecutable(fileNativeBin)
                    if (fileNativeBin.canExecute())
                        return fileNativeBin
                }
            }
            var folder = Build.SUPPORTED_ABIS.first()
            val javaArch = System.getProperty("os.arch")

            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86"
            }

            destLocalFile?.let {
                if (loadFromZip(appSourceDir, libName, destLocalFile, folder)) {
                    return destLocalFile
                }
            }

        } catch (e: Throwable) {
            logger.e(e, "loading native binary")
        }

        return null
    }

}