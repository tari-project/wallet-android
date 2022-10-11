package com.tari.android.wallet.service.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.util.WalletUtil

class WalletServiceLauncher(
    private val context: Context,
    val walletConfig: WalletConfig,
    val tariSettingsSharedRepository: TariSettingsSharedRepository
) {
    fun startIfExist() {
        if (WalletUtil.walletExists(walletConfig)) {
            startService()
        }
    }

    fun start() {
        if (tariSettingsSharedRepository.backgroundServiceTurnedOn ||
            !tariSettingsSharedRepository.backgroundServiceTurnedOn && TariWalletApplication.INSTANCE.get()?.isInForeground == true
        ) {
            startService()
        }
    }

    private fun startService() = ContextCompat.startForegroundService(context, getStartIntent(context))

    fun stop() = ContextCompat.startForegroundService(context, getStopIntent(context))

    fun stopAndDelete() = ContextCompat.startForegroundService(context, getStopAndDeleteIntent(context))

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

    private fun getStartIntent(context: Context) = Intent(context, WalletService::class.java).also { it.action = startAction }

    private fun getStopIntent(context: Context) = Intent(context, WalletService::class.java).also { it.action = stopAction }

    private fun getStopAndDeleteIntent(context: Context) = Intent(context, WalletService::class.java).also { it.action = stopAndDeleteAction }


    companion object {
        // intent actions
        const val startAction = "START_SERVICE"
        const val stopAction = "STOP_SERVICE"
        const val stopAndDeleteAction = "STOP_SERVICE_AND_DELETE_WALLET"
    }

}