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

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.addressPoisoning.AddressPoisoningPrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.SecurityStagesPrefRepository
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.model.Base58
import com.tari.android.wallet.model.TariWalletAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */

@Singleton
class CorePrefRepository @Inject constructor(
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkPrefRepository,
    private val backupSettingsRepository: BackupPrefRepository,
    private val baseNodeSharedRepository: BaseNodePrefRepository,
    private val yatSharedRepository: YatPrefRepository,
    private val torSharedRepository: TorPrefRepository,
    private val tariSettingsSharedRepository: TariSettingsPrefRepository,
    private val securityStagesRepository: SecurityStagesPrefRepository,
    private val sentryPrefRepository: SentryPrefRepository,
    private val securityPrefRepository: SecurityPrefRepository,
    private val addressPoisoningSharedRepository: AddressPoisoningPrefRepository,
) : CommonPrefRepository(networkRepository) {

    private object Key {
        const val WALLET_ADDRESS_BASE58 = "tari_wallet_public_key_hex_string"
        const val EMOJI_ID = "tari_wallet_emoji_id_"
        const val ALIAS = "tari_wallet_surname_"
        const val ANON_ID = "ANON_ID"
        const val ONBOARDING_STARTED = "tari_wallet_onboarding_started"
        const val ONBOARDING_AUTH_SETUP_COMPLETED = "tari_wallet_onboarding_auth_setup_completed"
        const val ONBOARDING_AUTH_SETUP_STARTED = "tari_wallet_onboarding_auth_setup_started"
        const val ONBOARDING_COMPLETED = "tari_wallet_onboarding_completed"
        const val ONBOARDING_DISPLAYED_AT_HOME = "tari_wallet_onboarding_displayed_at_home"
        const val NEED_TO_SHOW_RECOVERY_SUCCESS_DIALOG = "NEED_TO_SHOW_RECOVERY_SUCCESS_DIALOG"
        const val IS_DATA_CLEARED = "tari_is_data_cleared"
        const val KEEP_SCREEN_AWAKE_WHEN_RESTORE = "KEEP_SCREEN_AWAKE_WHEN_RESTORE"
        const val AIRDROP_TOKEN = "airdrop_token"
        const val AIRDROP_REFRESH_TOKEN = "AIRDROP_REFRESH_TOKEN"
    }

    var walletAddressBase58: Base58? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.WALLET_ADDRESS_BASE58))

    var emojiId: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.EMOJI_ID))

    var alias: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.ALIAS))

    var onboardingStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.ONBOARDING_STARTED))

    var onboardingCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.ONBOARDING_COMPLETED))

    var onboardingAuthSetupStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.ONBOARDING_AUTH_SETUP_STARTED))

    var onboardingAuthSetupCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.ONBOARDING_AUTH_SETUP_COMPLETED))

    val onboardingAuthWasInterrupted: Boolean
        get() = onboardingAuthSetupStarted && (!onboardingAuthSetupCompleted || securityPrefRepository.pinCode == null)

    val onboardingWasInterrupted: Boolean
        get() = onboardingStarted && !onboardingCompleted

    var onboardingDisplayedAtHome: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.ONBOARDING_DISPLAYED_AT_HOME))

    var needToShowRecoverySuccessDialog: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.NEED_TO_SHOW_RECOVERY_SUCCESS_DIALOG),
        defValue = false,
    )

    var isDataCleared: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(Key.IS_DATA_CLEARED), true)

    var keepScreenAwakeWhenRestore: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.KEEP_SCREEN_AWAKE_WHEN_RESTORE),
        defValue = true,
    )

    var airdropToken: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.AIRDROP_TOKEN))
    var airdropRefreshToken: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.AIRDROP_REFRESH_TOKEN))
    var airdropAnonId: String? by SharedPrefStringDelegate(sharedPrefs, this, formatKey(Key.ANON_ID))

    val walletAddress: TariWalletAddress
        get() = walletAddressBase58?.let { TariWalletAddress.fromBase58(it) } ?: error("Wallet address is not set to shared preferences")

    /**
     * Sometimes the wallet address is not set to the shared preferences (e.g. after a wallet removing).
     * TODO: Investigate why the app accesses the wallet address when it is not set
     */
    fun walletAddressExists(): Boolean = walletAddressBase58 != null

    fun clear() {
        baseNodeSharedRepository.clear()
        backupSettingsRepository.clear()
        yatSharedRepository.clear()
        torSharedRepository.clear()
        tariSettingsSharedRepository.clear()
        securityStagesRepository.clear()
        sentryPrefRepository.clear()
        securityPrefRepository.clear()
        addressPoisoningSharedRepository.clear()
        walletAddressBase58 = null
        emojiId = null
        onboardingStarted = false
        onboardingCompleted = false
        onboardingAuthSetupStarted = false
        onboardingAuthSetupCompleted = false
        onboardingDisplayedAtHome = false
        keepScreenAwakeWhenRestore = true
        airdropAnonId = null
        airdropToken = null
        airdropRefreshToken = null
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