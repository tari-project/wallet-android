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

import com.tari.android.wallet.application.Network
import com.tari.android.wallet.model.MicroTari
import java.math.BigInteger

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
        const val xShortDurationMs = 100L
        const val shortDurationMs = 300L
        const val mediumDurationMs = 600L
        const val longDurationMs = 1000L
        const val xLongDurationMs = 1500L
        const val xxLongDurationMs = 2000L

        const val scrollDepthShadowViewMaxOpacity = 0.75f

        const val emojiIdChunkSeparatorRelativeScale = 0.9f
        const val emojiIdChunkSeparatorLetterSpacing = 1f

        object Button {
            const val clickScaleAnimFullScale = 1f
            const val clickScaleAnimSmallScale = .88f
            const val clickScaleAnimDurationMs = 140L
            const val clickScaleAnimReturnDurationMs = 170L
            const val clickScaleAnimStartOffset = 0L
            const val clickScaleAnimReturnStartOffset = 120L
        }

        object Home {
            const val startupAnimDurationMs = 1500L
            const val digitAnimDurationMs = 700L
            const val digitShrinkExpandAnimDurationMs = 200L
            const val welcomeAnimationDurationMs = 800L
            const val showTariBotDialogDelayMs = 2000L
        }

        object CreateEmojiId {
            const val helloTextAnimDurationMs = 800L
            const val whiteBgAnimDurationMs = 1000L
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
            const val yourEmojiIdTextAnimDelayMs = 500L
            const val yourEmojiIdTextAnimDurationMs = 300L
        }

        object CreateWallet {
            const val removeFragmentDelayDuration = 1200L
            const val tariTextAnimViewDurationMs = 1600L
            const val viewContainerFadeOutDurationMs = 800L
            const val introductionBottomViewsFadeOutDelay = 1100L
        }

        object AddAmount {
            const val numPadDigitEnterAnimDurationMs = 90L
        }

        object AddNoteAndSend {
            const val preKeyboardHideWaitMs = 500L
            const val continueToFinalizeSendTxDelayMs = 2000L

        }

        object FinalizeSendTx {
            const val lottieAnimStartDelayMs = 400L
            const val textAppearAnimStartDelayMs = 200L
            const val successfulInfoFadeOutAnimStartDelayMs = 3130L
        }

        object Splash {
            const val createWalletStartUpDelayMs = 3000L
        }

        object Auth {
            const val viewFadeAnimDelayMs = 200L
            const val localAuthAnimDurationMs = 800L
            const val bottomViewsFadeOutDelay = 1200L
        }
    }

    /**
     * Wallet constants.
     */
    object Wallet {
        val network = Network.STIBBONS
        const val torPort = 18101
        const val maxNumberOfRollingLogFiles = 2
        const val rollingLogFileMaxSizeBytes = 10 * 1024 * 1024
        const val discoveryTimeoutSec = 20L
        const val storeAndForwardMessageDurationSec = 10800L
        const val emojiIdLength = 33
        const val emojiFormatterChunkSize = 3
        const val walletDBName: String = "tari_wallet_db"
        const val walletDBFullFileName: String = "$walletDBName.sqlite3"
        const val faucetServerUrl = "https://faucet.tari.com"
        const val pushNotificationServerUrl = "https://push.tari.com"
        const val deepLinkURLPrefix = "tari://"
        const val pendingTxExpirationPeriodHours = 3 * 24
        const val backupDelayMs = 60 * 1000L
        const val backupRetryPeriodMs = 0L
        const val maxBackupRetries = 2
        val defaultFeePerGram = MicroTari(BigInteger.valueOf(10))
    }

}
