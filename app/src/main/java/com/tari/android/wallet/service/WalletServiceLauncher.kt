package com.tari.android.wallet.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.util.WalletUtil

class WalletServiceLauncher(private val context: Context) {

    private lateinit var sharedPrefsRepository: SharedPrefsRepository

    constructor(sharedPrefsRepository: SharedPrefsRepository, context: Context) : this(context) {
        this.sharedPrefsRepository = sharedPrefsRepository
    }

    companion object {
        // intent actions
        internal const val startAction = "START_SERVICE"
        internal const val stopAction = "STOP_SERVICE"
        internal const val stopAndDeleteAction = "STOP_SERVICE_AND_DELETE_WALLET"
    }

    fun startIfExist() {
        if (WalletUtil.walletExists(context.applicationContext)) {
            startService()
        }
    }

    fun start() {
        if (sharedPrefsRepository.backgroundServiceTurnedOn ||
            !sharedPrefsRepository.backgroundServiceTurnedOn && TariWalletApplication.INSTANCE.get()?.isInForeground == true
        ) {
            startService()
        }
    }

    private fun startService() {
        ContextCompat.startForegroundService(
            context,
            getStartIntent(context)
        )
    }

    fun stop() {
        ContextCompat.startForegroundService(
            context,
            getStopIntent(context)
        )
    }

    /**
     * Deletes the wallet and stops the wallet service.
     */
    fun stopAndDelete() {
        ContextCompat.startForegroundService(
            context,
            getStopAndDeleteIntent(context)
        )
    }

    fun startOnAppForegrounded() {
        if (!sharedPrefsRepository.backgroundServiceTurnedOn) {
            start()
        }
    }

    fun stopOnAppBackgrounded() {
        if (!sharedPrefsRepository.backgroundServiceTurnedOn) {
            stop()
        }
    }

    private fun getStartIntent(context: Context) =
        Intent(context, WalletService::class.java).also {
            it.action = startAction
        }

    private fun getStopIntent(context: Context) =
        Intent(context, WalletService::class.java).also {
            it.action = stopAction
        }

    private fun getStopAndDeleteIntent(context: Context) =
        Intent(context, WalletService::class.java).also {
            it.action = stopAndDeleteAction
        }
}