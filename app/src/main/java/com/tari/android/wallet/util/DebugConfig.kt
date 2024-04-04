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

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.model.TariWalletAddress
import yat.android.lib.YatIntegration

/**
 *  Constants used for developing and debugging.
 */
@Suppress("ConstPropertyName", "KotlinConstantConditions")
object DebugConfig {

    private const val _mockedTurned = false
    val mockedDataEnabled = _mockedTurned && isDebug() // TODO split this flag to multiple different types of mocked data

    const val isChatEnabled = false

    private const val _useYatSandbox = false
    val yatEnvironment = if (_useYatSandbox && isDebug()) YatEnvironment.SANDBOX else YatEnvironment.PRODUCTION

    // Needed for testing resetting wallet on the network changed
    private const val _useStagenetNetwork = false
    val useStagenetNetwork = _useStagenetNetwork && isDebug()

    private fun isDebug() = BuildConfig.BUILD_TYPE == "debug"
}

object MockDataStub {
    private const val EMOJI_ID =
        "\uD83C\uDF34\uD83C\uDF0D\uD83C\uDFB5\uD83C\uDFBA\uD83D\uDDFD\uD83C\uDF37\uD83D\uDE91\uD83C\uDF45\uD83D\uDC60\uD83C\uDF1F\uD83D\uDC8C\uD83D\uDE97\uD83D\uDC40\uD83D\uDD29\uD83C\uDF08\uD83D\uDC1D\uD83C\uDF37\uD83C\uDF70\uD83C\uDF38\uD83C\uDF81\uD83C\uDF55\uD83D\uDEBF\uD83D\uDC34\uD83D\uDCA6\uD83D\uDE0E\uD83D\uDEAA\uD83C\uDFE0\uD83D\uDD29\uD83C\uDFE0\uD83D\uDE82\uD83C\uDFBA\uD83C\uDFC6\uD83C\uDFB3"
    private const val HEX = "C05575BE00EF016A209B1F493D9027B0E330F3E25FE89BBE6FA66D966EE5B6356"

    private val EMOJI_ID_ZERO = (0..25).map { "\uD83C\uDF00" }.joinToString("")
    private const val HEX_ZERO = TariWalletAddress.HEX_ZERO_66

    val WALLET_ADDRESS = TariWalletAddress(hexString = HEX, emojiId = EMOJI_ID)
    val WALLET_ADDRESS_ZERO = TariWalletAddress(HEX_ZERO, EMOJI_ID_ZERO)
}

object YatEnvironment {
    val SANDBOX = YatIntegration.Environment("https://a.yat.fyi/", "https://yat.fyi/")
    val PRODUCTION = YatIntegration.Environment("https://a.y.at/", "https://y.at/")
}