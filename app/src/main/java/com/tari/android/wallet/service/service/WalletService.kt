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
package com.tari.android.wallet.service.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.AppStateHandler
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletStarted
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.ServiceRestartBroadcastReceiver
import com.tari.android.wallet.service.service.WalletServiceLauncher.Companion.START_ACTION
import com.tari.android.wallet.service.service.WalletServiceLauncher.Companion.STOP_ACTION
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.screen.settings.logs.LogFilesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
class WalletService : Service() {

    @Inject
    lateinit var app: TariWalletApplication

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var appStateHandler: AppStateHandler

    private var lifecycleObserver: ServiceLifecycleCallbacks? = null

    private lateinit var wallet: FFIWallet

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    private val logger
        get() = Logger.t(WalletService::class.simpleName)

    override fun onCreate() {
        super.onCreate()
        DiContainer.appComponent.inject(this)

        serviceScope.launch {
            appStateHandler.appEvent.collect { event ->
                when (event) {
                    is AppStateHandler.AppEvent.AppBackgrounded,
                    is AppStateHandler.AppEvent.AppForegrounded -> LogFilesManager(walletConfig).manage()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null because this service is not meant to be bound to
        return null
    }

    /**
     * Called when a component decides to start or stop the foreground wallet service.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground()
        when (intent.action) {
            START_ACTION -> startService(intent.getStringArrayExtra(WalletServiceLauncher.ARG_SEED_WORDS)?.toList())
            STOP_ACTION -> stopService(startId)
            else -> throw RuntimeException("Unexpected intent action: ${intent.action}")
        }
        return START_NOT_STICKY
    }

    private fun startService(seedWords: List<String>?) {
        //todo total crutch. Service is auto-creating during the bind func. Need to refactor this first
        DiContainer.appComponent.inject(this)

        serviceScope.launch {
            walletManager.doOnWalletStarted {
                onWalletStarted(it)
            }
        }
        walletManager.start(seedWords)
        logger.i("Wallet service started")
    }

    private fun startForeground() {
        // start service & post foreground service notification
        startForeground(NOTIFICATION_ID, notificationHelper.buildForegroundServiceNotification())
    }

    private fun stopService(startId: Int) {
        // stop service
        stopForeground(STOP_FOREGROUND_REMOVE)
        walletManager.stop()
        stopSelfResult(startId)
        // stop wallet manager on a separate thread & unsubscribe from events
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
    }

    private fun onWalletStarted(ffiWallet: FFIWallet) {
        wallet = ffiWallet
        lifecycleObserver = ServiceLifecycleCallbacks(wallet)
        Handler(Looper.getMainLooper()).post { ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!) }
        walletManager.onWalletStarted()
    }

    /**
     * A broadcast is made on destroy to get the service running again.
     */
    override fun onDestroy() {
        logger.i("Wallet service destroyed")
        sendBroadcast(Intent(this, ServiceRestartBroadcastReceiver::class.java))
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}