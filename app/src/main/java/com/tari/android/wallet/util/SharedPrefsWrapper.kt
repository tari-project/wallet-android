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

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tari.android.wallet.service.faucet.TestnetTariUTXOKey
import de.adorsys.android.securestoragelibrary.SecurePreferences
import org.joda.time.DateTime
import java.math.BigInteger

private val String.toPreservedByteArray: ByteArray
    get() {
        return this.toByteArray(Charsets.ISO_8859_1)
    }

private val ByteArray.toPreservedString: String
    get() {
        return String(this, Charsets.ISO_8859_1)
    }

/**
 * Provides easy access to the shared preferences.
 *
 * @author The Tari Development Team
 */
class SharedPrefsWrapper(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) {

    private object Key {
        const val privateKeyHexString = "tari_wallet_private_key_hex_string"
        const val publicKeyHexString = "tari_wallet_public_key_hex_string"
        const val isAuthenticated = "tari_wallet_is_authenticated"
        const val emojiId = "tari_wallet_emoji_id_"
        const val onboardingStarted = "tari_wallet_onboarding_started"
        const val onboardingAuthSetupCompleted = "tari_wallet_onboarding_auth_setup_completed"
        const val onboardingAuthSetupStarted = "tari_wallet_onboarding_auth_setup_started"
        const val onboardingCompleted = "tari_wallet_onboarding_completed"
        const val onboardingDisplayedAtHome = "tari_wallet_onboarding_displayed_at_home"
        const val torBinPath = "tari_wallet_tor_bin_path"
        const val torIdentity = "tari_wallet_tor_identity"
        const val baseNodePublicKeyHexKey = "tari_wallet_base_node_public_key_hex"
        const val baseNodeAddressKey = "tari_wallet_base_node_address"
        const val faucetTestnetTariRequestCompleted =
            "tari_wallet_faucet_testnet_tari_request_completed"
        const val testnetTariUTXOListKey = "tari_wallet_testnet_tari_utxo_key_list"
        const val firstTestnetUTXOTxId = "tari_wallet_first_testnet_utxo_tx_id"
        const val secondTestnetUTXOTxId = "tari_wallet_second_testnet_utxo_tx_id"
        const val lastSuccessfulBackupDate = "tari_wallet_last_successful_backup_date"
        const val backupFailureDate = "tari_wallet_backup_failure_date"
        const val scheduledBackupDate = "tari_wallet_scheduled_backup_date"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
    }

    // TODO(nyarian): remove value on null possibly?
    var privateKeyHexString: String?
        get() = SecurePreferences.getStringValue(context, Key.privateKeyHexString, null)
        set(value) =
            value?.let { SecurePreferences.setValue(context, Key.privateKeyHexString, it) }
                ?: Unit

    var publicKeyHexString: String?
        get() = sharedPrefs.getString(Key.publicKeyHexString, null)
        set(value) = sharedPrefs.edit().run {
            putString(Key.publicKeyHexString, value)
            apply()
        }

    var isAuthenticated: Boolean
        get() = sharedPrefs.getBoolean(Key.isAuthenticated, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.isAuthenticated, value)
            apply()
        }

    var emojiId: String?
        get() = sharedPrefs.getString(Key.emojiId, null)
        set(value) = sharedPrefs.edit().run {
            putString(Key.emojiId, value)
            apply()
        }

    var onboardingStarted: Boolean
        get() = sharedPrefs.getBoolean(Key.onboardingStarted, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.onboardingStarted, value)
            apply()
        }

    var onboardingCompleted: Boolean
        get() = sharedPrefs.getBoolean(Key.onboardingCompleted, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.onboardingCompleted, value)
            apply()
        }

    var onboardingAuthSetupStarted: Boolean
        get() = sharedPrefs.getBoolean(Key.onboardingAuthSetupStarted, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.onboardingAuthSetupStarted, value)
            apply()
        }


    var onboardingAuthSetupCompleted: Boolean
        get() = sharedPrefs.getBoolean(Key.onboardingAuthSetupCompleted, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.onboardingAuthSetupCompleted, value)
            apply()
        }


    val onboardingAuthWasInterrupted: Boolean
        get() = onboardingAuthSetupStarted && !onboardingAuthSetupCompleted

    val onboardingWasInterrupted: Boolean
        get() = onboardingStarted && !onboardingCompleted

    var onboardingDisplayedAtHome: Boolean
        get() = sharedPrefs.getBoolean(Key.onboardingDisplayedAtHome, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.onboardingDisplayedAtHome, value)
            apply()
        }

    var torBinPath: String?
        get() = sharedPrefs.getString(Key.torBinPath, null)
        set(value) = sharedPrefs.edit().run {
            putString(Key.torBinPath, value)
            apply()
        }

    var torIdentity: ByteArray?
        get() = sharedPrefs.getString(Key.torIdentity, null)?.toPreservedByteArray
        set(value) = sharedPrefs.edit().run {
            putString(Key.torIdentity, value?.toPreservedString)
            apply()
        }

    var baseNodePublicKeyHex: String?
        get() = sharedPrefs.getString(Key.baseNodePublicKeyHexKey, null)
        set(value) = sharedPrefs.edit().run {
            putString(Key.baseNodePublicKeyHexKey, value)
            apply()
        }

    var baseNodeAddress: String?
        get() = sharedPrefs.getString(Key.baseNodeAddressKey, null)
        set(value) = sharedPrefs.edit().run {
            putString(Key.baseNodeAddressKey, value)
            apply()
        }

    var faucetTestnetTariRequestCompleted: Boolean
        get() = sharedPrefs.getBoolean(Key.faucetTestnetTariRequestCompleted, false)
        set(value) = sharedPrefs.edit().run {
            putBoolean(Key.faucetTestnetTariRequestCompleted, value)
            apply()
        }

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

    var firstTestnetUTXOTxId: BigInteger?
        get() = sharedPrefs.getString(Key.firstTestnetUTXOTxId, null)?.run(::BigInteger)
        set(value) = value?.let { v ->
            sharedPrefs.edit().run {
                putString(Key.firstTestnetUTXOTxId, v.toString())
                apply()
            }
        } ?: Unit

    var secondTestnetUTXOTxId: BigInteger?
        get() = sharedPrefs.getString(Key.secondTestnetUTXOTxId, null)?.run(::BigInteger)
        set(value) = value?.let { v ->
            sharedPrefs.edit().run {
                putString(Key.secondTestnetUTXOTxId, v.toString())
                apply()
            }
        } ?: Unit

    var lastSuccessfulBackupDate: DateTime?
        get() = sharedPrefs.getLong(Key.lastSuccessfulBackupDate, -1L)
            .let { if (it == -1L) null else DateTime(it) }
        set(value) = sharedPrefs.edit().apply {
            if (value == null) remove(Key.lastSuccessfulBackupDate)
            else putLong(Key.lastSuccessfulBackupDate, value.millis)
        }.apply()

    var backupFailureDate: DateTime?
        get() = sharedPrefs.getLong(Key.backupFailureDate, -1L)
            .let { if (it == -1L) null else DateTime(it) }
        set(value) = sharedPrefs.edit().apply {
            if (value == null) remove(Key.backupFailureDate)
            else putLong(Key.backupFailureDate, value.millis)
        }.apply()

    var scheduledBackupDate: DateTime?
        get() = sharedPrefs.getLong(Key.scheduledBackupDate, -1L)
            .let { if (it == -1L) null else DateTime(it) }
        set(value) = sharedPrefs.edit().apply {
            if (value == null) remove(Key.scheduledBackupDate)
            else putLong(Key.scheduledBackupDate, value.millis)
        }.apply()

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

    init {
        // for migration purposes, to avoid a second redundant faucet call:
        // faucetTestnetTariRequestCompleted was introduced  after firstTestnetUTXOTxId and
        // secondTestnetUTXOTxId properties
        if (firstTestnetUTXOTxId != null && secondTestnetUTXOTxId != null) {
            faucetTestnetTariRequestCompleted = true
        }
    }

    fun clean() {
        privateKeyHexString = null
        publicKeyHexString = null
        isAuthenticated = false
        emojiId = null
        onboardingStarted = false
        onboardingCompleted = false
        onboardingAuthSetupStarted = false
        onboardingAuthSetupCompleted = false
        onboardingDisplayedAtHome = false
        torBinPath = null
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
    }

}
