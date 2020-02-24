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

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */
class SharedPrefsWrapper(private val sharedPrefs: SharedPreferences) {

    private val privateKeyHexStringKey = "tari_wallet_private_key_hex_string"
    private val onboardingStartedKey = "tari_wallet_onboarding_started"
    private val onboardingCompletedKey = "tari_wallet_onboarding_completed"
    private val onboardingDisplayedAtHome = "tari_wallet_onboarding_displayed_at_home"

    fun getPrivateKeyHexString(): String? {
        return sharedPrefs.getString(privateKeyHexStringKey, null)
    }

    fun setPrivateKeyHexString(privateKeyHexString: String?) {
        sharedPrefs.edit().apply {
            putString(privateKeyHexStringKey, privateKeyHexString)
            apply()
        }
    }

    fun getOnboardingStarted(): Boolean {
        return sharedPrefs.getBoolean(onboardingStartedKey, false)
    }

    fun setOnboardingStarted(onboardingStarted: Boolean) {
        sharedPrefs.edit().apply {
            putBoolean(onboardingStartedKey, onboardingStarted)
            apply()
        }
    }

    fun getOnboardingCompleted(): Boolean {
        return sharedPrefs.getBoolean(onboardingCompletedKey, false)
    }

    fun setOnboardingCompleted(onboardingCompleted: Boolean) {
        sharedPrefs.edit().apply {
            putBoolean(onboardingCompletedKey, onboardingCompleted)
            apply()
        }
    }

    fun getOnboardingDisplayedAtHome(): Boolean {
        return sharedPrefs.getBoolean(onboardingDisplayedAtHome, false)
    }

    fun setOnboardingDisplayedAtHome(onboardingCompleted: Boolean) {
        sharedPrefs.edit().apply {
            putBoolean(onboardingDisplayedAtHome, onboardingCompleted)
            apply()
        }
    }

    fun clean() {
        setPrivateKeyHexString(null)
        setOnboardingStarted(false)
        setOnboardingCompleted(false)
    }

}