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
import android.net.Uri
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.*
import org.joda.time.DateTime
import java.math.BigInteger
import java.nio.charset.Charset
import kotlin.random.Random

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */
//todo Need to thing about reactive realization
class SharedPrefsRepository(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val baseNodeSharedRepository: BaseNodeSharedRepository
) {

    private object Key {
        const val publicKeyHexString = "tari_wallet_public_key_hex_string"
        const val isAuthenticated = "tari_wallet_is_authenticated"
        const val emojiId = "tari_wallet_emoji_id_"
        const val onboardingStarted = "tari_wallet_onboarding_started"
        const val onboardingAuthSetupCompleted = "tari_wallet_onboarding_auth_setup_completed"
        const val onboardingAuthSetupStarted = "tari_wallet_onboarding_auth_setup_started"
        const val onboardingCompleted = "tari_wallet_onboarding_completed"
        const val onboardingDisplayedAtHome = "tari_wallet_onboarding_displayed_at_home"
        const val torBinPath = "tari_wallet_tor_bin_path"
        const val faucetTestnetTariRequestCompleted = "tari_wallet_faucet_testnet_tari_request_completed"
        const val testnetTariUTXOListKey = "tari_wallet_testnet_tari_utxo_key_list"
        const val firstTestnetUTXOTxId = "tari_wallet_first_testnet_utxo_tx_id"
        const val secondTestnetUTXOTxId = "tari_wallet_second_testnet_utxo_tx_id"
        const val lastSuccessfulBackupDate = "tari_wallet_last_successful_backup_date"
        const val backupFailureDate = "tari_wallet_backup_failure_date"
        const val scheduledBackupDate = "tari_wallet_scheduled_backup_date"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val walletDatabasePassphrase = "tari_wallet_database_passphrase"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val network = "tari_wallet_network"
        const val isRestoredWallet = "tari_is_restored_wallet"
        const val hasVerifiedSeedWords = "tari_has_verified_seed_words"
        const val backgroundServiceTurnedOnKey = "tari_background_service_turned_on"
        const val isDataCleared = "tari_is_data_cleared"
    }

    var publicKeyHexString: String? by SharedPrefStringDelegate(sharedPrefs, Key.publicKeyHexString)

    var isAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.isAuthenticated)

    var emojiId: String? by SharedPrefStringDelegate(sharedPrefs, Key.emojiId)

    var onboardingStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.onboardingStarted)

    var onboardingCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.onboardingCompleted)

    var onboardingAuthSetupStarted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.onboardingAuthSetupStarted)

    var onboardingAuthSetupCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.onboardingAuthSetupCompleted)

    val onboardingAuthWasInterrupted: Boolean
        get() = onboardingAuthSetupStarted && !onboardingAuthSetupCompleted

    val onboardingWasInterrupted: Boolean
        get() = onboardingStarted && !onboardingCompleted

    var onboardingDisplayedAtHome: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.onboardingDisplayedAtHome)

    var torBinPath: String? by SharedPrefStringDelegate(sharedPrefs, Key.torBinPath)

    var faucetTestnetTariRequestCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.faucetTestnetTariRequestCompleted)

    var testnetTariUTXOKeyList: TestnetUtxoList? by SharedPrefGsonDelegate(sharedPrefs, Key.testnetTariUTXOListKey, TestnetUtxoList::class.java)

    var firstTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, Key.firstTestnetUTXOTxId)

    var secondTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, Key.secondTestnetUTXOTxId)

    var lastSuccessfulBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.lastSuccessfulBackupDate)

    var backupFailureDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.backupFailureDate)

    var scheduledBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.scheduledBackupDate)

    val backupIsEnabled: Boolean
        get() = (lastSuccessfulBackupDate != null)

    var backupPassword: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, Key.backupPassword)

    var localBackupFolderURI: Uri? by SharedPrefGsonDelegate(sharedPrefs, Key.localBackupFolderURI, Uri::class.java)

    var network: Network? by SharedPrefGsonDelegate(sharedPrefs, Key.network, Network::class.java, Network.WEATHERWAX)

    var databasePassphrase: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, Key.walletDatabasePassphrase)

    var isRestoredWallet: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.isRestoredWallet)

    var hasVerifiedSeedWords: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.hasVerifiedSeedWords)

    var backgroundServiceTurnedOn: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.backgroundServiceTurnedOnKey, true)

    var isDataCleared: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.isDataCleared, true)

    init {
        // for migration purposes, to avoid a second redundant faucet call:
        // faucetTestnetTariRequestCompleted was introduced  after firstTestnetUTXOTxId and
        // secondTestnetUTXOTxId properties
        if (firstTestnetUTXOTxId != null && secondTestnetUTXOTxId != null) {
            faucetTestnetTariRequestCompleted = true
        }
    }

    fun clear() {
        baseNodeSharedRepository.clear()
        publicKeyHexString = null
        isAuthenticated = false
        emojiId = null
        onboardingStarted = false
        onboardingCompleted = false
        onboardingAuthSetupStarted = false
        onboardingAuthSetupCompleted = false
        onboardingDisplayedAtHome = false
        torBinPath = null
        faucetTestnetTariRequestCompleted = false
        testnetTariUTXOKeyList = null
        firstTestnetUTXOTxId = null
        secondTestnetUTXOTxId = null
        lastSuccessfulBackupDate = null
        backupFailureDate = null
        scheduledBackupDate = null
        backupPassword = null
        localBackupFolderURI = null
        network = null
        isRestoredWallet = false
        hasVerifiedSeedWords = false
        backgroundServiceTurnedOn = true
        databasePassphrase = null
    }

    fun generateDatabasePassphrase() {
        val array = ByteArray(32)
        Random.nextBytes(array)
        val generatedString = String(array, Charset.forName("UTF-8"))
        databasePassphrase = generatedString
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
}
