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
package com.tari.android.wallet.data.sharedPrefs

import android.content.Context
import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetFaucetRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRepository
import com.tari.android.wallet.yat.YatSharedRepository
import kotlin.random.Random

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */
//todo Need to thing about reactive realization

class SharedPrefsRepository(
    context: Context,
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkRepository,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val baseNodeSharedRepository: BaseNodeSharedRepository,
    private val testnetFaucetRepository: TestnetFaucetRepository,
    private val yatSharedRepository: YatSharedRepository,
    private val torSharedRepository: TorSharedRepository,
    private var tariSettingsSharedRepository: TariSettingsSharedRepository
) : CommonRepository(networkRepository) {

    private object Key {
        const val publicKeyHexString = "tari_wallet_public_key_hex_string"
        const val isAuthenticated = "tari_wallet_is_authenticated"
        const val emojiId = "tari_wallet_emoji_id_"
        const val onboardingStarted = "tari_wallet_onboarding_started"
        const val onboardingAuthSetupCompleted = "tari_wallet_onboarding_auth_setup_completed"
        const val onboardingAuthSetupStarted = "tari_wallet_onboarding_auth_setup_started"
        const val onboardingCompleted = "tari_wallet_onboarding_completed"
        const val onboardingDisplayedAtHome = "tari_wallet_onboarding_displayed_at_home"
        const val walletDatabasePassphrase = "tari_wallet_database_passphrase"
        const val isDataCleared = "tari_is_data_cleared"
    }

    var publicKeyHexString: String? by SharedPrefStringDelegate(sharedPrefs, formatKey(Key.publicKeyHexString))

    var isAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.isAuthenticated))

    var emojiId: String? by SharedPrefStringDelegate(sharedPrefs, formatKey(Key.emojiId))

    var onboardingStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.onboardingStarted))

    var onboardingCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.onboardingCompleted))

    var onboardingAuthSetupStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.onboardingAuthSetupStarted))

    var onboardingAuthSetupCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.onboardingAuthSetupCompleted))

    val onboardingAuthWasInterrupted: Boolean
        get() = onboardingAuthSetupStarted && !onboardingAuthSetupCompleted

    val onboardingWasInterrupted: Boolean
        get() = onboardingStarted && !onboardingCompleted

    var onboardingDisplayedAtHome: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.onboardingDisplayedAtHome))

    var databasePassphrase: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, formatKey(Key.walletDatabasePassphrase))

    var isDataCleared: Boolean by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Key.isDataCleared), true)

    fun clear() {
        baseNodeSharedRepository.clear()
        backupSettingsRepository.clear()
        testnetFaucetRepository.clear()
        yatSharedRepository.clear()
        torSharedRepository.clear()
        tariSettingsSharedRepository.clear()
        publicKeyHexString = null
        isAuthenticated = false
        emojiId = null
        onboardingStarted = false
        onboardingCompleted = false
        onboardingAuthSetupStarted = false
        onboardingAuthSetupCompleted = false
        onboardingDisplayedAtHome = false
        databasePassphrase = null
    }

    fun generateDatabasePassphrase(): String {
        val generatedString = java.lang.StringBuilder()

        while (generatedString.length < 32) {
            val nextChar = Char(Random.nextInt(Char.MIN_VALUE.code, Char.MAX_VALUE.code))
            if (isBrokenCharForPassphrase(nextChar)) continue
            generatedString.append(nextChar)
        }
        return generatedString.toString()
    }

    // Runs when user manually clear the application data
    fun checkIfIsDataCleared(): Boolean {
        val isCleared = isDataCleared
        if (isCleared) {
            clear()
            isDataCleared = false
        }
        return isCleared
    }

    companion object {
        fun isBrokenCharForPassphrase(char: Char): Boolean = char.code == 0 || char.code in (55296..57343)
    }
}