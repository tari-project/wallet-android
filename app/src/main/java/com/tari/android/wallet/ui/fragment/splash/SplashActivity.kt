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
package com.tari.android.wallet.ui.fragment.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletNotReady
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.fragment.auth.AuthActivity
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash screen activity.
 *
 * @author The Tari Development Team
 */
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var sharedPrefsRepository: CorePrefRepository

    @Inject
    lateinit var securityPrefRepository: SecurityPrefRepository

    @Inject
    lateinit var networkRepository: NetworkPrefRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var walletManager: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback { }

        if (sharedPrefsRepository.checkIfIsDataCleared()) {
            walletServiceLauncher.stopAndDelete()
        }

        if (!networkRepository.isCurrentNetworkSupported()) {
            changeNetwork()
        }

        val exists = WalletUtil.walletExists(walletConfig) && sharedPrefsRepository.onboardingAuthSetupCompleted
        if (WalletUtil.walletExists(walletConfig) && !sharedPrefsRepository.onboardingAuthSetupCompleted) {
            // in cases interrupted restoration
            WalletUtil.clearWalletFiles(walletConfig.getWalletFilesDirPath())
            sharedPrefsRepository.clear()
        }
        if (securityPrefRepository.pinCode == null) {
            launchActivity(OnboardingFlowActivity::class.java)
            return
        }
        launchActivity(if (exists) AuthActivity::class.java else OnboardingFlowActivity::class.java)
    }

    private fun changeNetwork() {
        networkRepository.setDefaultNetworkAsCurrent()

        lifecycle.coroutineScope.launch {
            walletManager.doOnWalletNotReady {
                EventBus.clear()
                DiContainer.reInitContainer()
            }
        }

        walletServiceLauncher.stop()
    }

    private fun <T : Activity> launchActivity(destination: Class<T>) {
        val intent = Intent(this, destination)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        this.intent.data?.let(intent::setData)
        startActivity(intent)
        finish()
    }
}
