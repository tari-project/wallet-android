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

import com.tari.android.wallet.util.extension.toMicroTari

/**
 * Contains application-wide constant values.
 *
 * @author The Tari Development Team
 */
object Constants {

    /**
     * UI constants.
     */
    object UI {

        const val xShortDurationMs = 100L
        const val shortDurationMs = 300L
        const val mediumDurationMs = 600L
        const val longDurationMs = 1000L
        const val xLongDurationMs = 1500L

        const val EMOJI_ID_CHUNK_SEPARATOR_RELATIVE_SCALE = 0.9f
        const val EMOJI_ID_CHUNK_SEPARATOR_LETTER_SPACING = 1f

        object Button {
            const val clickScaleAnimFullScale = 1f
            const val clickScaleAnimSmallScale = .88f
            const val clickScaleAnimDurationMs = 140L
            const val clickScaleAnimReturnDurationMs = 170L
            const val clickScaleAnimStartOffset = 0L
            const val clickScaleAnimReturnStartOffset = 120L
        }

        object CreateEmojiId {
            const val helloTextAnimDurationMs = 800L
            const val whiteBgAnimDurationMs = 1000L
            const val titleShortAnimDelayMs = 40L
            const val createEmojiButtonAnimDelayMs = 300L
            const val awesomeTextAnimDurationMs = 600L
            const val shortAlphaAnimDuration = 300L
            const val viewOverlapDelayMs = 150L
            const val createEmojiViewAnimDurationMs = 1200L
            const val walletCreationFadeOutAnimDurationMs = 1000L
            const val walletCreationFadeOutAnimDelayMs = 300L
            const val continueButtonAnimDurationMs = 800L
            const val emojiIdCreationViewAnimDurationMs = 1000L
            const val emojiIdImageViewAnimDelayMs = 200L
            const val yourEmojiIdTextAnimDelayMs = 300L
            const val yourEmojiIdTextAnimDurationMs = 300L
        }

        object AddAmount {
            const val numPadDigitEnterAnimDurationMs = 90L
        }

        object FinalizeSendTx {
            const val lottieAnimStartDelayMs = 400L
            const val textAppearAnimStartDelayMs = 200L
            const val successfulInfoFadeOutAnimStartDelayMs = 1500L
        }

        object Auth {
            const val viewFadeAnimDelayMs = 200L
            const val localAuthAnimDurationMs = 800L
        }
    }

    /**
     * Wallet constants.
     */
    object Wallet {
        const val EMOJI_FORMATTER_CHUNK_SIZE = 3
        val DEFAULT_FEE_PER_GRAM = 10.toMicroTari()
    }
}
