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
package com.tari.android.wallet.application

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tari.android.wallet.di.ApplicationComponent
import com.tari.android.wallet.di.ApplicationModule
import com.tari.android.wallet.di.DaggerApplicationComponent
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.network.NetworkConnectionStateReceiver
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.service.WalletServiceLauncher
import net.danlew.android.joda.JodaTimeAndroid
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Named

/**
 * Main application class.
 *
 * @author The Tari Development Team
 */
internal class TariWalletApplication : Application(), LifecycleObserver {

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var connectionStateReceiver: NetworkConnectionStateReceiver

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    lateinit var appComponent: ApplicationComponent
    private val activityLifecycleCallbacks = ActivityLifecycleCallbacks()
    var isInForeground = false
        private set

    init {
        System.loadLibrary("native-lib")
    }

    val currentActivity: Activity?
        get() = activityLifecycleCallbacks.currentActivity

    override fun onCreate() {
        super.onCreate()
        INSTANCE = WeakReference(this)

        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        Logger.addLogAdapter(AndroidLogAdapter())
        JodaTimeAndroid.init(this)

        appComponent = initDagger(this)
        appComponent.inject(this)

        notificationHelper.createNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // user should authenticate every time the app starts up
        sharedPrefsRepository.isAuthenticated = false

        registerReceiver(connectionStateReceiver, connectionStateReceiver.intentFilter)

        // track app download
        tracker.download(this)
    }

    private fun initDagger(app: TariWalletApplication): ApplicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(app))
            .build()

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Logger.d("App in background.")
        isInForeground = false
        walletServiceLauncher.stopOnAppBackgrounded()
        EventBus.post(Event.App.AppBackgrounded())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Logger.d("App in foreground.")
        isInForeground = true
        walletServiceLauncher.startOnAppForegrounded()
        EventBus.post(Event.App.AppForegrounded())
    }


    companion object {

        @Volatile
        var INSTANCE: WeakReference<TariWalletApplication> = WeakReference(null)
            private set
    }
}
