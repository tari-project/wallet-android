package com.tari.android.wallet.service.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.walletManager.WalletFileUtil
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository

/**
 * Wallet service launcher. Starts the wallet service that starts the wallet.
 */
class WalletServiceLauncher(
    private val context: Context,
    val walletConfig: WalletConfig,
    val tariSettingsSharedRepository: TariSettingsPrefRepository
) {
    fun startIfWalletExists(seedWords: List<String>? = null) {
        if (WalletFileUtil.walletExists(walletConfig)) {
            startService(seedWords)
        }
    }

    // We can't pass a FFISeedWords object to the service, so we pass the seed words as a list of strings
    fun start(seedWords: List<String>? = null) {
        val backgroundServiceTurnedOn = tariSettingsSharedRepository.backgroundServiceTurnedOn
        val appInForeground = TariWalletApplication.INSTANCE.get()?.isInForeground == true
        if (backgroundServiceTurnedOn || appInForeground) {
            startService(seedWords)
        }
    }

    private fun startService(seedWords: List<String>?) =
        ContextCompat.startForegroundService(context, Intent(context, WalletService::class.java).also {
            it.action = START_ACTION
            it.putExtra(ARG_SEED_WORDS, seedWords?.toTypedArray())
        })

    fun stop() = ContextCompat.startForegroundService(context, Intent(context, WalletService::class.java).also { it.action = STOP_ACTION })

    fun startOnAppForegrounded() {
        if (!tariSettingsSharedRepository.backgroundServiceTurnedOn) {
            start()
        }
    }

    fun stopOnAppBackgrounded() {
        if (!tariSettingsSharedRepository.backgroundServiceTurnedOn) {
            stop()
        }
    }

    companion object {
        const val START_ACTION = "START_SERVICE"
        const val STOP_ACTION = "STOP_SERVICE"
        const val ARG_SEED_WORDS = "ARG_SEED_WORDS"
    }
}