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

import android.content.SharedPreferences
import de.adorsys.android.securestoragelibrary.SecurePreferences

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */
class SharedPrefsWrapper(private val sharedPrefs: SharedPreferences) {

    private object Key {
        const val privateKeyHexStringKey = "tari_wallet_private_key_hex_string"
        const val publicKeyHexStringKey = "tari_wallet_public_key_hex_string"
        const val emojiIdKey = "tari_wallet_emoji_id_"
        const val onboardingStartedKey = "tari_wallet_onboarding_started"
        const val onboardingCompletedKey = "tari_wallet_onboarding_completed"
        const val onboardingDisplayedAtHomeKey = "tari_wallet_onboarding_displayed_at_home"
    }

    var privateKeyHexString: String?
        get() {
            return SecurePreferences.getStringValue(Key.privateKeyHexStringKey, null)
        }
        set(value) {
            if (value != null) {
                SecurePreferences.setValue(Key.privateKeyHexStringKey, value)
            }
        }

    var publicKeyHexString: String?
        get() {
            return sharedPrefs.getString(Key.publicKeyHexStringKey, null)
        }
        set(value) {
            sharedPrefs.edit().apply {
                putString(Key.publicKeyHexStringKey, value)
                apply()
            }
        }

    var emojiId: String?
        get() {
            return sharedPrefs.getString(Key.emojiIdKey, null)
        }
        set(value) {
            sharedPrefs.edit().apply {
                putString(Key.emojiIdKey, value)
                apply()
            }
        }

    var onboardingStarted: Boolean
        get() {
            return sharedPrefs.getBoolean(Key.onboardingStartedKey, false)
        }
        set(value) {
            sharedPrefs.edit().apply {
                putBoolean(Key.onboardingStartedKey, value)
                apply()
            }
        }

    var onboardingCompleted: Boolean
        get() {
            return sharedPrefs.getBoolean(Key.onboardingCompletedKey, false)
        }
        set(value) {
            sharedPrefs.edit().apply {
                putBoolean(Key.onboardingCompletedKey, value)
                apply()
            }
        }

    val onboardingWasInterrupted: Boolean
        get() {
            return onboardingStarted && !onboardingCompleted
        }

    var onboardingDisplayedAtHome: Boolean
        get() {
            return sharedPrefs.getBoolean(Key.onboardingDisplayedAtHomeKey, false)
        }
        set(value) {
            sharedPrefs.edit().apply {
                putBoolean(Key.onboardingDisplayedAtHomeKey, value)
                apply()
            }
        }

    fun clean() {
        publicKeyHexString = ""
        emojiId = ""
        onboardingStarted = false
        onboardingCompleted = false
        onboardingDisplayedAtHome = false
    }

}