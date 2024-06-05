package com.tari.android.wallet.tor

import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.freehaven.tor.control.TorControlConnection
import java.io.File
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorProxyControl @Inject constructor(
    private val torConfig: TorConfig,
    private val torProxyStateHandler: TorProxyStateHandler,
) {
    companion object {
        private const val INITIALIZATION_CHECK_TIMEOUT_MILLIS = 15000L
        private const val INITIALIZATION_CHECK_RETRY_PERIOD_MILLIS = 500L

        /**
         * Check Tor status every 5 seconds.
         */
        private const val STATUS_CHECK_PERIOD_SECS = 5L
    }

    private var monitoringStartTimeMs = 0L

    /**
     * Timer to check Tor status.
     */
    private var timerSubscription: Disposable? = null
    private val logger
        get() = Logger.t(TorProxyControl::class.java.simpleName)

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
        logger.i("Shutdown Tor proxy")
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
            if (initializationElapsedMs > INITIALIZATION_CHECK_TIMEOUT_MILLIS) {
                logger.e(throwable, "Failed to connect to Tor proxy, timed out")
                updateState(TorProxyState.Failed(throwable))
            } else {
                logger.i("Failed to connect to Tor proxy, will retry $initializationElapsedMs ms")
                logger.i(throwable.toString())
                timerSubscription = Observable.timer(INITIALIZATION_CHECK_RETRY_PERIOD_MILLIS, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { connectToTor() }
            }
        }
    }

    private fun checkTorStatus() {
        try {
            val bootstrapStatus = controlConnection.torBootstrapStatus()
            if (bootstrapStatus != null) {
                if (bootstrapStatus.progress == 100 && bootstrapStatus.summary == "Done") {
                    updateState(TorProxyState.Running(bootstrapStatus))
                } else {
                    updateState(TorProxyState.Initializing(bootstrapStatus))
                }
                // schedule timer
                timerSubscription = Observable.timer(STATUS_CHECK_PERIOD_SECS, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { checkTorStatus() }
            } else {
                updateState(TorProxyState.Failed(Throwable("Tor not running")))
            }
        } catch (throwable: Throwable) {
            logger.i("tor state during fall: ${torProxyStateHandler.torProxyState.value}")
            logger.e(throwable, "Tor proxy has failed")
            updateState(TorProxyState.Failed(throwable))
        }
    }

    private fun TorControlConnection?.torBootstrapStatus(): TorBootstrapStatus? {
        return this?.let {
            val phaseLogLine = this.getInfo("status/bootstrap-phase")
            return TorBootstrapStatus.from(phaseLogLine)
        }
    }

    private fun updateState(newState: TorProxyState) {
        if (newState != torProxyStateHandler.torProxyState.value) {
            logger.i("Tor proxy state update: $newState")
            torProxyStateHandler.updateState(newState)
        }
    }
}