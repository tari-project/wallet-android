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
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TariSettingsPrefRepository @Inject constructor(
    // TODO rename to SettingsPrefRepository
    private val sharedPrefs: SharedPreferences,
    private val networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private object Key {
        const val IS_RESTORED_WALLET = "tari_is_restored_wallet"
        const val HAS_VERIFIED_SEED_WORDS = "tari_has_verified_seed_words"
        const val SCREEN_RECORDING_TURNED_ON_KEY = "tari_screen_recording_turned_on"
        const val THEME_KEY = "tari_theme_key"
    }

    var isRestoredWallet: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.IS_RESTORED_WALLET),
    )

    var hasVerifiedSeedWords: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.HAS_VERIFIED_SEED_WORDS),
    )

    var screenRecordingTurnedOn: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.SCREEN_RECORDING_TURNED_ON_KEY),
        defValue = false
    )

    var currentTheme: TariTheme by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = Key.THEME_KEY,
        type = TariTheme::class.java,
        defValue = TariTheme.AppBased,
    )

    fun clear() {
        isRestoredWallet = false
        hasVerifiedSeedWords = false
    }
}