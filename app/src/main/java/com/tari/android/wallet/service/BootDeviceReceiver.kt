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
package com.tari.android.wallet.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationModule
import com.tari.android.wallet.service.service.WalletServiceLauncher

/**
 * This receiver is responsible for starting the service after boot finish.
 *
 * @author The Tari Development Team
 */
class BootDeviceReceiver : BroadcastReceiver() {

    private val logger
        get() = Logger.t(BootDeviceReceiver::class.simpleName)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        // not allowed action starts from android 12 dio to new restriction
        // https://developer.android.com/guide/components/foreground-services#background-start-restriction-exemptions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        logger.i("Boot device broadcast received")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val sharedPreferences = context.getSharedPreferences(ApplicationModule.sharedPrefsFileName, Context.MODE_PRIVATE)
            val networkRepository = NetworkPrefRepository(sharedPreferences)
            val tariSettingsSharedRepository = TariSettingsPrefRepository(sharedPreferences, networkRepository)
            WalletServiceLauncher(context, WalletConfig(context, networkRepository), tariSettingsSharedRepository).startIfWalletExists()
        }
    }
}