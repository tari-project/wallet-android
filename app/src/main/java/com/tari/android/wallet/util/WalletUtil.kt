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

import android.net.Uri
import com.tari.android.wallet.extension.toMicroTari
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.QRScanData
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

    val amountFormatter = DecimalFormat("#,##0.00").apply {
        roundingMode = RoundingMode.FLOOR
    }

    val feeFormatter = DecimalFormat("#,##0.0000").apply {
        roundingMode = RoundingMode.CEILING
    }

    /**
     * Calculates transaction fee.
     * See https://github.com/tari-project/tari/issues/1058.
     */
    fun calculateTxFee(
        baseCost: Int = 500,
        numInputs: Int = 1,
        numOutputs: Int = 2,
        r: Int = 250
    ): MicroTari {
        val fee = baseCost + (numInputs + 4 * numOutputs) * r
        return fee.toMicroTari()
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

    fun getQRContent(publicKey: String, emojiId: String): String {
        return Constants.Wallet.QR_DEEP_LINK_URL + "/$publicKey?emoji_id=$emojiId"
    }

    /*
    *  PublicKey and emojiId data from qr scan result
    * */
    fun getQrScanData(content: String): QRScanData {
        val url = Uri.parse(content)
        val pathSegment = url.pathSegments
        var publickKey = ""
        if (pathSegment.isNotEmpty()) {
            publickKey = pathSegment[0]
        }
        val emojiId = url.getQueryParameter("emoji_id") ?: ""

        return QRScanData(publickKey, emojiId)
    }

    fun getLogFilesFromDirectory(dirPath: String): List<File> {
        val root = File(dirPath)
        val files: MutableList<File>? = root.listFiles()?.toMutableList()
        val filteredFile = files?.filter { it.extension == "log" }?.toMutableList()
        filteredFile?.sortByDescending { it.lastModified() }
        return filteredFile ?: Collections.emptyList()
    }
}