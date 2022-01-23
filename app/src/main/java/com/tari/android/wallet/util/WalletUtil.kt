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
package com.tari.android.wallet.util

import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.model.MicroTari
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

/**
 * Wallet utility functions.
 *
 * @author The Tari Development Team
 */
internal object WalletUtil {

    val balanceFormatter = DecimalFormat("#,##0.00").apply {
        roundingMode = RoundingMode.FLOOR
    }

    val amountFormatter = DecimalFormat("#,##0.00####").apply {
        roundingMode = RoundingMode.FLOOR
    }

    /**
     * Utility function to clear all previous wallet files.
     */
    fun clearWalletFiles(path: String): Boolean {
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

    fun getEmojiIdDeepLink(emojiId: String, network: Network): String {
        return "${Constants.Wallet.deepLinkURLPrefix}${network.uriComponent}/${DeepLink.Type.EMOJI_ID.uriComponent}/$emojiId"
    }

    fun getPublicKeyHexDeepLink(publicKeyHex: String, network: Network): String {
        return "${Constants.Wallet.deepLinkURLPrefix}${network.uriComponent}/${DeepLink.Type.PUBLIC_KEY_HEX.uriComponent}/$publicKeyHex"
    }

    fun generateFullQrCodeDeepLink(publicKeyHex: String, network: Network, amount: MicroTari, alias: String): String {
        return "${Constants.Wallet.deepLinkURLPrefix}${network.uriComponent}/${DeepLink.Type.PUBLIC_KEY_HEX.uriComponent}/$publicKeyHex?alias=$alias&amount=${amount.tariValue}"
    }

    fun getLogFilesFromDirectory(dirPath: String): List<File> {
        val root = File(dirPath)
        if (!root.isDirectory) return Collections.emptyList()
        val files = root.listFiles()!!.toMutableList()
        val filteredFiles = files.filter { it.extension == "log" }.toMutableList()
        filteredFiles.sortBy { it.name }
        // actual log file will be at the end of the sorted list due to the log file rolling
        // naming convention, move it to the top
        if (filteredFiles.size > 1) {
            val currentLogFile = filteredFiles.removeAt(filteredFiles.size - 1)
            filteredFiles.add(0, currentLogFile)
        }
        return filteredFiles
    }

    fun walletExists(walletConfig: WalletConfig): Boolean = File(walletConfig.getWalletFilesDirPath(), walletConfig.walletDBFullFileName).exists()
}