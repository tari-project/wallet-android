package com.tari.android.wallet.tor

import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.EventBus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.freehaven.tor.control.TorControlConnection
import java.io.File
import java.net.Socket
import java.util.concurrent.TimeUnit

class TorProxyControl(private val torConfig: TorConfig) {

    private val initializationCheckTimeoutMillis = 15000L
    private val initializationCheckRetryPeriodMillis = 500L
    private var monitoringStartTimeMs = 0L

    /**
     * Check Tor status every 5 seconds.
     */
    private val statusCheckPeriodSecs = 5L

    /**
     * Timer to check Tor status.
     */
    private var timerSubscription: Disposable? = null

    private lateinit var socket: Socket
    private lateinit var controlConnection: TorControlConnection

    @Synchronized
    fun startMonitoringTor() {
        connectToTor()
    }

    @Synchronized
    fun shutdownTor() {
        timerSubscription?.dispose()
        if (this::controlConnection.isInitialized) {
            runCatching { controlConnection.shutdownTor("SHUTDOWN") }
        }
    }

    private fun connectToTor() {
        monitoringStartTimeMs = System.currentTimeMillis()
        try {
            socket = Socket(torConfig.controlHost, torConfig.controlPort)
            controlConnection = TorControlConnection(socket)
            controlConnection.authenticate(File(torConfig.cookieFilePath).readBytes())
            checkTorStatus()
        } catch (throwable: Throwable) {
            val initializationElapsedMs = System.currentTimeMillis() - monitoringStartTimeMs
            if (initializationElapsedMs > initializationCheckTimeoutMillis) {
                Logger.e("Failed to connect to Tor proxy, timed out: %s", throwable.message)
                updateState(TorProxyState.Failed(throwable))
            } else {
                Logger.e("Failed to connect to Tor proxy, will retry: %s", throwable.message)
                timerSubscription = Observable.timer(initializationCheckRetryPeriodMillis, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { connectToTor() }
            }
        }
    }

    private fun checkTorStatus() {
        try {
            val bootstrapStatus = isTorRunning(controlConnection)
            if (bootstrapStatus != null) {
                if (bootstrapStatus.progress == 100 && bootstrapStatus.summary == "Done") {
                    updateState(TorProxyState.Running(bootstrapStatus))
                } else {
                    updateState(TorProxyState.Initializing(bootstrapStatus))
                }
                // schedule timer
                timerSubscription = Observable.timer(statusCheckPeriodSecs, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { checkTorStatus() }
            } else {
                updateState(TorProxyState.Failed(Throwable("Tor not running")))
            }
        } catch (throwable: Throwable) {
            Logger.e("Tor proxy has failed: %s", throwable.message)
            updateState(TorProxyState.Failed(throwable))
        }
    }

    private fun isTorRunning(controlConnection: TorControlConnection?): TorBootstrapStatus? {
        val phaseLogLine = controlConnection?.getInfo("status/bootstrap-phase") ?: return null
        return TorBootstrapStatus.from(phaseLogLine)
    }

    private fun updateState(newState: TorProxyState) {
        EventBus.torProxyState.post(newState)
    }
}