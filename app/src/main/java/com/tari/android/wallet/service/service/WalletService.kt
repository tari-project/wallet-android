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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetFaucetRepository
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.ServiceRestartBroadcastReceiver
import com.tari.android.wallet.service.faucet.TestnetFaucetService
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.service.service.WalletServiceLauncher.Companion.startAction
import com.tari.android.wallet.service.service.WalletServiceLauncher.Companion.stopAction
import com.tari.android.wallet.service.service.WalletServiceLauncher.Companion.stopAndDeleteAction
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
class WalletService : Service(), LifecycleObserver {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var app: TariWalletApplication

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var testnetFaucetService: TestnetFaucetService

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var baseNodeSharedPrefsRepository: BaseNodeSharedRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var testnetFaucetRepository: TestnetFaucetRepository

    private lateinit var wallet: FFIWallet
    private lateinit var serviceStub: TariWalletServiceStub

    private val logger
        get() = Logger.t(WalletService::class.simpleName)

    /**
     * Check for expired txs every 30 minutes.
     */
    private val expirationCheckPeriodMinutes = Minutes.minutes(30)

    /**
     * Switch to low power mode 3 minutes after the app gets backgrounded.
     */
    private val backgroundLowPowerModeSwitchMinutes = Minutes.minutes(3)

    /**
     * Timer to trigger the expiration checks.
     */
    private var txExpirationCheckSubscription: Disposable? = null

    private var lowPowerModeSubscription: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        DiContainer.appComponent.inject(this)
    }

    /**
     * Called when a component decides to start or stop the foreground wallet service.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground()
        when (intent.action) {
            startAction -> startService()
            stopAction -> stopService(startId)
            stopAndDeleteAction -> {
                //todo total crutch. Service is auto-creating during the bind func. Need to refactor this first
                DiContainer.appComponent.inject(this)
                stopService(startId)
                deleteWallet()
            }
            else -> throw RuntimeException("Unexpected intent action: ${intent.action}")
        }
        return START_NOT_STICKY
    }

    private fun startService() {
        //todo total crutch. Service is auto-creating during the bind func. Need to refactor this first
        DiContainer.appComponent.inject(this)
        // start wallet manager on a separate thread & listen to events
        EventBus.walletState.subscribe(this, this::onWalletStateChanged)
        Thread { walletManager.start() }.start()
        logger.i("Wallet service started")
    }

    private fun startForeground() {
        // start service & post foreground service notification
        val notification = notificationHelper.buildForegroundServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopService(startId: Int) {
        // stop service
        stopForeground(true)
        stopSelfResult(startId)
        // stop wallet manager on a separate thread & unsubscribe from events
        EventBus.walletState.unsubscribe(this)
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        GlobalScope.launch { backupManager.turnOff(deleteExistingBackups = false) }
        Thread {
            walletManager.stop()
        }.start()
    }

    private fun deleteWallet() {
        WalletUtil.clearWalletFiles(walletConfig.getWalletFilesDirPath())
        sharedPrefsWrapper.clear()
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.Started) {
            wallet = FFIWallet.instance!!
            val impl = FFIWalletListenerImpl(wallet, backupManager, notificationHelper, notificationService, app, baseNodeSharedPrefsRepository, baseNodes)
            serviceStub = TariWalletServiceStub(wallet, testnetFaucetRepository, testnetFaucetService, baseNodeSharedPrefsRepository, resourceManager, impl)
            wallet.listener = impl
            EventBus.walletState.unsubscribe(this)
            scheduleExpirationCheck()
            backupManager.initialize()
            val handler = Handler(Looper.getMainLooper())
            handler.post { ProcessLifecycleOwner.get().lifecycle.addObserver(this) }
            EventBus.walletState.post(WalletState.Running)
        }
    }

    private fun scheduleExpirationCheck() {
        txExpirationCheckSubscription =
            Observable
                .timer(expirationCheckPeriodMinutes.minutes.toLong(), TimeUnit.MINUTES)
                .repeat()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    cancelExpiredPendingInboundTxs()
                    cancelExpiredPendingOutboundTxs()
                }
    }

    override fun onBind(intent: Intent?): IBinder {
        logger.i("Wallet service bound")
        return serviceStub
    }

    override fun onUnbind(intent: Intent?): Boolean {
        logger.i("Wallet service unbound")
        return super.onUnbind(intent)
    }

    /**
     * A broadcast is made on destroy to get the service running again.
     */
    override fun onDestroy() {
        logger.i("Wallet service destroyed")
        txExpirationCheckSubscription?.dispose()
        sendBroadcast(
            Intent(this, ServiceRestartBroadcastReceiver::class.java)
        )
        super.onDestroy()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // schedule low power mode
        lowPowerModeSubscription = Observable
            .timer(backgroundLowPowerModeSwitchMinutes.minutes.toLong(), TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { switchToLowPowerMode() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        switchToNormalPowerMode()
    }

    private fun switchToNormalPowerMode() {
        logger.i("Switch to normal power mode")
        lowPowerModeSubscription?.dispose()
        try {
            wallet.setPowerModeNormal()
        } catch (e: FFIException) {
            logger.e(e, "Switching to normal power mode failed")
        }
    }

    private fun switchToLowPowerMode() {
        logger.i("Switch to low power mode")
        try {
            wallet.setPowerModeLow()
        } catch (e: FFIException) {
            logger.e(e, "Switching to low power mode failed")
        }
    }

    /**
     * Cancels expired pending inbound transactions.
     * Expiration period is defined by Constants.Wallet.pendingTxExpirationPeriodHours
     */
    private fun cancelExpiredPendingInboundTxs() {
        val pendingInboundTxs = wallet.getPendingInboundTxs()
        val pendingInboundTxsLength = pendingInboundTxs.getLength()
        val now = DateTime.now().toLocalDateTime()
        for (i in 0 until pendingInboundTxsLength) {
            val tx = pendingInboundTxs.getAt(i)
            val txDate = DateTime(tx.getTimestamp().toLong() * 1000L).toLocalDateTime()
            val hoursPassed = Hours.hoursBetween(txDate, now).hours
            if (hoursPassed >= Constants.Wallet.pendingTxExpirationPeriodHours) {
                wallet.cancelPendingTx(tx.getId())
            }
            tx.destroy()
        }
        pendingInboundTxs.destroy()
    }

    /**
     * Cancels expired pending outbound transactions.
     * Expiration period is defined by Constants.Wallet.pendingTxExpirationPeriodHours
     */
    private fun cancelExpiredPendingOutboundTxs() {
        val pendingOutboundTxs = wallet.getPendingOutboundTxs()
        val pendingOutboundTxsLength = wallet.getPendingOutboundTxs().getLength()
        val now = DateTime.now().toLocalDateTime()
        for (i in 0 until pendingOutboundTxsLength) {
            val tx = pendingOutboundTxs.getAt(i)
            val txDate = DateTime(tx.getTimestamp().toLong() * 1000L).toLocalDateTime()
            val hoursPassed = Hours.hoursBetween(txDate, now).hours
            if (hoursPassed >= Constants.Wallet.pendingTxExpirationPeriodHours) {
                wallet.cancelPendingTx(tx.getId())
            }
            tx.destroy()
        }

        pendingOutboundTxs.destroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        object KeyValueStorageKeys {
            const val NETWORK = "SU7FM2O6Q3BU4XVN7HDD"
        }
    }
}

