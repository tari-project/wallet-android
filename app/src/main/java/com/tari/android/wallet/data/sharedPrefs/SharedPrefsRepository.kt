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
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBigIntegerDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefDateTimeDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.model.BaseNodeValidationResult
import com.tari.android.wallet.service.faucet.TestnetTariUTXOKey
import de.adorsys.android.securestoragelibrary.SecurePreferences
import org.joda.time.DateTime
import java.math.BigInteger
import javax.inject.Inject

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */

class SharedPrefsRepository(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
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
        const val baseNodeLastSyncResult = "tari_wallet_base_node_last_sync_result"
        const val baseNodeIsUserCustom = "tari_wallet_base_node_is_user_custom"
        const val baseNodeNameKey = "tari_wallet_base_node_name"
        const val baseNodePublicKeyHexKey = "tari_wallet_base_node_public_key_hex"
        const val baseNodeAddressKey = "tari_wallet_base_node_address"
        const val faucetTestnetTariRequestCompleted = "tari_wallet_faucet_testnet_tari_request_completed"
        const val testnetTariUTXOListKey = "tari_wallet_testnet_tari_utxo_key_list"
        const val firstTestnetUTXOTxId = "tari_wallet_first_testnet_utxo_tx_id"
        const val secondTestnetUTXOTxId = "tari_wallet_second_testnet_utxo_tx_id"
        const val lastSuccessfulBackupDate = "tari_wallet_last_successful_backup_date"
        const val backupFailureDate = "tari_wallet_backup_failure_date"
        const val scheduledBackupDate = "tari_wallet_scheduled_backup_date"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val network = "tari_wallet_network"
        const val isRestoredWallet = "tari_is_restored_wallet"
        const val hasVerifiedSeedWords = "tari_has_verified_seed_words"
        const val backgroundServiceTurnedOnKey = "tari_background_service_turned_on"
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

    var baseNodeLastSyncResult: BaseNodeValidationResult?
        get() = try {
            BaseNodeValidationResult.map(sharedPrefs.getInt(Key.baseNodeLastSyncResult, -1))
        } catch (exception: Exception) {
            null
        }
        set(value) = sharedPrefs.edit().run {
            putInt(Key.baseNodeLastSyncResult, value?.status ?: -1)
            apply()
        }

    var baseNodeIsUserCustom: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.baseNodeIsUserCustom)

    var baseNodeName: String? by SharedPrefStringDelegate(sharedPrefs, Key.baseNodeNameKey)

    var baseNodePublicKeyHex: String? by SharedPrefStringDelegate(sharedPrefs, Key.baseNodePublicKeyHexKey)

    var baseNodeAddress: String? by SharedPrefStringDelegate(sharedPrefs, Key.baseNodeAddressKey)

    var faucetTestnetTariRequestCompleted: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.faucetTestnetTariRequestCompleted)

    var testnetTariUTXOKeyList: List<TestnetTariUTXOKey>
        get() {
            val json = sharedPrefs.getString(Key.testnetTariUTXOListKey, "[]")
            val listType = object : TypeToken<List<TestnetTariUTXOKey>>() {}.type
            return GsonBuilder().create().fromJson(json, listType)
        }
        set(value) = sharedPrefs.edit().run {
            putString(Key.testnetTariUTXOListKey, GsonBuilder().create().toJson(value))
            apply()
        }

    var firstTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, Key.firstTestnetUTXOTxId)

    var secondTestnetUTXOTxId: BigInteger? by SharedPrefBigIntegerDelegate(sharedPrefs, Key.secondTestnetUTXOTxId)

    var lastSuccessfulBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.lastSuccessfulBackupDate)

    var backupFailureDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.backupFailureDate)

    var scheduledBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, Key.scheduledBackupDate)

    val backupIsEnabled: Boolean
        get() = (lastSuccessfulBackupDate != null)

    var backupPassword: CharArray?
        get() = SecurePreferences.getStringValue(context, Key.backupPassword, null)?.toCharArray()
        set(value) =
            if (value == null) SecurePreferences.removeValue(context, Key.backupPassword)
            else SecurePreferences.setValue(context, Key.backupPassword, value.joinToString(""))

    var localBackupFolderURI: Uri?
        get() = sharedPrefs.getString(Key.localBackupFolderURI, null)?.let(Uri::parse)
        set(value) = sharedPrefs.edit().apply {
            if (value == null) remove(Key.localBackupFolderURI)
            else putString(Key.localBackupFolderURI, value.toString())
        }.apply()

    var network: Network?
        get() = try {
            Network.from(sharedPrefs.getString(Key.network, null) ?: "")
        } catch (exception: Exception) {
            null
        }
        set(value) = sharedPrefs.edit().run {
            putString(Key.network, value?.uriComponent)
            apply()
        }

    var isRestoredWallet: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.isRestoredWallet)

    var hasVerifiedSeedWords: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.hasVerifiedSeedWords)

    var backgroundServiceTurnedOn: Boolean by SharedPrefBooleanDelegate(sharedPrefs, Key.backgroundServiceTurnedOnKey, true)

    init {
        // for migration purposes, to avoid a second redundant faucet call:
        // faucetTestnetTariRequestCompleted was introduced  after firstTestnetUTXOTxId and
        // secondTestnetUTXOTxId properties
        if (firstTestnetUTXOTxId != null && secondTestnetUTXOTxId != null) {
            faucetTestnetTariRequestCompleted = true
        }
    }

    fun clear() {
        publicKeyHexString = null
        isAuthenticated = false
        emojiId = null
        onboardingStarted = false
        onboardingCompleted = false
        onboardingAuthSetupStarted = false
        onboardingAuthSetupCompleted = false
        onboardingDisplayedAtHome = false
        torBinPath = null
        baseNodeLastSyncResult = null
        baseNodeIsUserCustom = false
        baseNodeName = null
        baseNodePublicKeyHex = null
        baseNodeAddress = null
        faucetTestnetTariRequestCompleted = false
        testnetTariUTXOKeyList = mutableListOf()
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
    }

}
