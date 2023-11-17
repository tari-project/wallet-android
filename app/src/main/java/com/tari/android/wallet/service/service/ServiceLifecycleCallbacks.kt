package com.tari.android.wallet.service.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIWallet
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.joda.time.Minutes
import java.util.concurrent.TimeUnit

class ServiceLifecycleCallbacks(private val wallet: FFIWallet): DefaultLifecycleObserver {

    /**
     * Switch to low power mode 3 minutes after the app gets backgrounded.
     */
    private val backgroundLowPowerModeSwitchMinutes = Minutes.minutes(3)

    private val logger: Printer
        get() = Logger.t(ServiceLifecycleCallbacks::class.simpleName)

    private var lowPowerModeSubscription: Disposable? = null

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // schedule low power mode
        lowPowerModeSubscription = Observable.timer(backgroundLowPowerModeSwitchMinutes.minutes.toLong(), TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { switchToLowPowerMode() }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        switchToNormalPowerMode()
    }

    private fun switchToNormalPowerMode() {
        logger.i("Switch to normal power mode")
        lowPowerModeSubscription?.dispose()
        try {
            wallet.setPowerModeNormal()
        } catch (e: FFIException) {
            logger.i(e.toString() + "Switching to normal power mode failed")
        }
    }

    private fun switchToLowPowerMode() {
        logger.i("Switch to low power mode")
        try {
            wallet.setPowerModeLow()
        } catch (e: FFIException) {
            logger.i(e.toString() + "Switching to low power mode failed")
        }
    }
}