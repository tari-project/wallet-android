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
package com.tari.android.wallet.tor

import android.content.Context
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Installs Tor resources from assets to the app files directory.
 * https://2019.www.torproject.org/docs/tor-manual.html.en
 *
 * @author The Tari Development Team
 */
class TorResourceInstaller(
    private val context: Context,
    private val sharedTorSharedRepository: TorSharedRepository,
    private val torConfig: TorConfig
) {

    private val appFilesDir: File = context.filesDir
    private val appDataDir: File = context.getDir(DIRECTORY_TOR_DATA, Context.MODE_PRIVATE)
    private val appNativeDir: File = File(context.applicationInfo.nativeLibraryDir)
    private val appSourceDir: File = File(context.applicationInfo.sourceDir)

    lateinit var fileTor: File
    lateinit var fileTorrcCustom: File
    lateinit var fileTorControlPort: File
    lateinit var fileTorrc: File

    fun installResources() {
        if (!appFilesDir.exists())
            appFilesDir.mkdirs()

        if (!appDataDir.exists())
            appDataDir.mkdirs()

        fileTorControlPort = File(appFilesDir, TOR_CONTROL_PORT_FILE)

        installGeoIPResources()

        fileTorrc = assetToFile(
            appFilesDir.absolutePath, COMMON_ASSET_KEY + TORRC_ASSET_KEY, TORRC_ASSET_KEY,
            isZipped = false,
            setExecutable = false
        )

        updateTorrcCustomFile()?.let { fileTorrcCustom = it }

        fileTor = File(appNativeDir, LIB_SO_NAME)

        if (fileTor.exists()) {
            if (fileTor.canExecute())
                return
            else {
                makeFileExecutable(fileTor)
                if (fileTor.canExecute())
                    return
            }

            val insStream: InputStream = FileInputStream(fileTor)
            streamToFile(insStream, fileTor, false, true)
            makeFileExecutable(fileTor)

            if (fileTor.exists() && fileTor.canExecute())
                return

            //it exists but we can't execute it, so copy it to a new path
            NativeLoader.loadNativeBinary(appNativeDir, appSourceDir, TOR_ASSET_KEY, File(appFilesDir, TOR_ASSET_KEY))?.let {
                if (it.exists())
                    makeFileExecutable(fileTor)

                if (fileTor.exists() && fileTor.canExecute()) {
                    fileTor = it
                }
            }
        }
    }

    /**
     * Install the Tor geo IP resources from assets to app files directory.
     */
    private fun installGeoIPResources() {
        assetToFile(appFilesDir.absolutePath, COMMON_ASSET_KEY + GEOIP_ASSET_KEY, GEOIP_ASSET_KEY)
        assetToFile(appFilesDir.absolutePath, COMMON_ASSET_KEY + GEOIP6_ASSET_KEY, GEOIP6_ASSET_KEY)
    }

    private fun assetToFile(filesPath: String, assetPath: String, assetKey: String, isZipped: Boolean = false, setExecutable: Boolean = false): File {
        val inputStream = context.assets.open(assetPath)
        val outFile = File(filesPath, assetKey)
        if (!outFile.exists()) {
            streamToFile(inputStream, outFile, isZipped = isZipped)
            if (setExecutable) {
                makeFileExecutable(outFile)
            }
        }
        return outFile
    }

    private fun streamToFile(inputStream: InputStream, outFile: File, append: Boolean = false, isZipped: Boolean = false) {
        FileOutputStream(outFile.absolutePath, append).use { outputStream ->
            (if (isZipped) ZipInputStream(inputStream).also { it.nextEntry } else inputStream).use { input ->
                var byteCount: Int
                val buffer = ByteArray(FILE_WRITE_BUFFER_SIZE)
                while (input.read(buffer).also { byteCount = it } > 0) {
                    outputStream.write(buffer, 0, byteCount)
                }
            }
        }
    }

    private fun updateTorrcCustomFile(): File? {

        val extraLines = StringBuffer().apply {
            append("AvoidDiskWrites 1\n")
            append("CookieAuthentication 1\n")
            append("ClientOnly 1\n")
            append("SocksPort ${torConfig.proxyPort}\n")
            append("ControlPort ${torConfig.controlHost}:${torConfig.controlPort}\n")
            append("Socks5ProxyUsername ${torConfig.sock5Username}\n")
            append("Socks5ProxyPassword ${torConfig.sock5Password}\n")

            sharedTorSharedRepository.currentTorBridges.orEmpty().forEach {
                append("Bridge ${it}\n")
                if (it.transportTechnology.isNotBlank()) {
                    append("ClientTransportPlugin ${it.transportTechnology} socks5 127.0.0.1:47351\n")
                }
            }
            if (sharedTorSharedRepository.currentTorBridges.orEmpty().isNotEmpty()) {
                append("UseBridges 1\n")
            }
        }

        val fileTorRcCustom = File(fileTorrc.absolutePath + ".custom")
        val success = updateTorConfigCustom(fileTorRcCustom, extraLines.toString())

        return if (success && fileTorRcCustom.exists()) fileTorRcCustom else null
    }

    private fun updateTorConfigCustom(fileTorRcCustom: File, extraLines: String?): Boolean {
        if (fileTorRcCustom.exists()) {
            fileTorRcCustom.delete()
        } else
            fileTorRcCustom.createNewFile()

        val fos = FileOutputStream(fileTorRcCustom, false)
        val ps = PrintStream(fos)
        ps.print(extraLines)
        ps.close()

        return true
    }

    companion object {
        const val DIRECTORY_TOR_DATA = "data"
        const val TOR_CONTROL_PORT_FILE = "control.txt"
        const val LIB_SO_NAME = "libtor.so"

        //geoip data file asset key
        const val GEOIP_ASSET_KEY = "geoip"
        const val GEOIP6_ASSET_KEY = "geoip6"

        //torrc (tor config file)
        const val TOR_ASSET_KEY = "libtor"
        const val TORRC_ASSET_KEY = "torrc"
        const val COMMON_ASSET_KEY = "tor/"

        var FILE_WRITE_BUFFER_SIZE = 1024

        fun makeFileExecutable(fileBin: File) {
            fileBin.setReadable(true)
            fileBin.setExecutable(true)
            fileBin.setWritable(false)
            fileBin.setWritable(true, true)
        }
    }
}