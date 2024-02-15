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
package com.tari.android.wallet.data.sharedPrefs.tariSettings

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariSettingsSharedRepository @Inject constructor(sharedPrefs: SharedPreferences, networkRepository: NetworkRepository) :
    CommonRepository(networkRepository) {

    private object Key {
        const val isRestoredWallet = "tari_is_restored_wallet"
        const val hasVerifiedSeedWords = "tari_has_verified_seed_words"
        const val backgroundServiceTurnedOnKey = "tari_background_service_turned_on"
        const val screenRecordingTurnedOnKey = "tari_screen_recording_turned_on"
        const val isOneSidePaymentEnabledKey = "is_one_side_payment_enabled"
        const val themeKey = "tari_theme_key"
    }

    var isRestoredWallet: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.isRestoredWallet))

    var hasVerifiedSeedWords: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.hasVerifiedSeedWords))

    var backgroundServiceTurnedOn: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.backgroundServiceTurnedOnKey), true)

    var screenRecordingTurnedOn: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.screenRecordingTurnedOnKey), false)

    var isOneSidePaymentEnabled: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.isOneSidePaymentEnabledKey), false)

    var currentTheme: TariTheme? by SharedPrefGsonDelegate(sharedPrefs, this, Key.themeKey, TariTheme::class.java, TariTheme.AppBased)

    fun clear() {
        isRestoredWallet = false
        hasVerifiedSeedWords = false
        backgroundServiceTurnedOn = true
    }
}