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
package com.tari.android.wallet.di

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.gyphy.GiphyAdapter
import dagger.Module
import dagger.Provides
import java.io.File
import javax.inject.Singleton

/**
 * Dagger basic application module for DI.
 *
 * @author The Tari Development Team
 */
@Module
class ApplicationModule(private val app: TariWalletApplication) {

    @Provides
    @Singleton
    fun provideApplication(): TariWalletApplication = app

    @Provides
    @Singleton
    fun provideContext(): Context = app

    @Provides
    @Singleton
    fun provideFilesDir(context: Context): File = context.filesDir

    @Provides
    @Singleton
    fun provideSharedPrefs(): SharedPreferences = app.getSharedPreferences(sharedPrefsFileName, MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideResourceManager(context: Context): ResourceManager = ResourceManager(context)

    @Provides
    @Singleton
    fun provideWalletServiceLauncher(
        context: Context,
        tariSettingsSharedRepository: TariSettingsPrefRepository,
        walletConfig: WalletConfig
    ): WalletServiceLauncher = WalletServiceLauncher(context, walletConfig, tariSettingsSharedRepository)

    @Provides
    @Singleton
    fun provideNotificationHelper(context: Context) = NotificationHelper(context)

    @Provides
    @Singleton
    fun provideClipboardManager(context: Context): ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideBiometricAuthService(context: Context): BiometricAuthenticationService =
        BiometricAuthenticationService(
            ContextCompat.getMainExecutor(context),
            BiometricManager.from(context),
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        )

    @Provides
    @Singleton
    fun provideGiphyAdapter(context: Context): GiphyAdapter = GiphyAdapter(context, BuildConfig.GIPHY_KEY)

    companion object {
        const val sharedPrefsFileName = "tari_wallet_shared_prefs"
    }
}
