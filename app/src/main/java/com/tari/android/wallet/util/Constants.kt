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

import com.tari.android.wallet.ffi.NetAddressString

/**
 * Contains application-wide constant values.
 *
 * @author The Tari Development Team
 */
internal object Constants {

    /**
     * UI constants.
     */
    object UI {

        const val keyboardHideWaitMs = 100L
        const val shortAnimDurationMs = 300L
        const val mediumAnimDurationMs = 600L
        const val longAnimDurationMs = 1000L

        const val scrollDepthShadowViewMaxOpacity = 0.8f

        object Button {
            const val clickScaleAnimFullScale = 1f
            const val clickScaleAnimSmallScale = .88f
            const val clickScaleAnimDurationMs = 170L
            const val clickScaleAnimReturnDurationMs = 170L
            const val clickScaleAnimStartOffset = 0L
            const val clickScaleAnimReturnStartOffset = 120L
        }

        object Home {
            const val startupAnimDurationMs = 1500L
            const val digitAnimDurationMs = 700L
            const val digitShrinkExpandAnimDurationMs = 200L
            const val welcomeAnimationDurationMs = 800L
            const val emptyWalletTxtFadeAnimDelayMs = 100L
            const val showEmptyWalletFadeAnimDurationMs = 400L
            const val showTariBotDialogDelayMs = 3000L
        }

        object CreateEmojiId {
            const val helloTextAnimDurationMs = 800L
            const val whiteBgAnimDurationMs = 1000L
            const val whiteBgAnimDelayMs = 200L
            const val titleShortAnimDelayMs = 40L
            const val createEmojiButtonAnimDelayMs = 300L
            const val awesomeTextAnimDurationMs = 600L
            const val shortAlphaAnimDuration = 300L
            const val viewChangeAnimDelayMs = 3000L
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

        object CreateWallet {
            const val showCreateEmojiIdWhiteBgDelayMs = 1500L
            const val removeFragmentDelayDuration = 1200L
            const val tariTextAnimViewDurationMs = 1600L
            const val startUpAnimDuration = 1000L
            const val titleTextAnimDelayDurationMs = 30L
            const val viewContainerFadeOutDurationMs = 800L
        }

        object AddAmount {
            const val numPadDigitEnterAnimDurationMs = 200L
        }

        object AddNoteAndSend {
            const val preKeyboardHideWaitMs = 500L
            const val postSendDelayMs = 3000L

        }

        object SendTxSuccessful {
            const val lottieAnimStartDelayMs = 400L
            const val textAppearAnimStartDelayMs = 500L
            const val textFadeOutAnimStartDelayMs = 4750L
        }

        object Splash {
            const val createWalletStartUpDelayMs = 3000L
        }
    }

    /**
     * Wallet constants.
     */
    object Wallet {
        internal const val emojiIdLength = 32
        internal const val emojiIdShortenedLength = 12
        internal const val emojiFormatterChunkSize = 4
        const val WALLET_DB_NAME: String = "tari_wallet_db"
        internal val WALLET_CONTROL_SERVICE_ADDRESS: NetAddressString =
            NetAddressString("127.0.0.1", 80)
        internal val WALLET_LISTENER_ADDRESS: NetAddressString = NetAddressString("0.0.0.0", 0)
        internal const val WALLET_SERVER_URL = "https://dropper.free.beeceptor.com"
    }

}