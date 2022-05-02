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
package com.tari.android.wallet.ui.fragment.onboarding.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.fragment.onboarding.inroduction.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletListener
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthListener
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateWallet
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * onBoarding activity class : contain  splash screen and loading sequence
 *
 * @author The Tari Development Team
 */
class OnboardingFlowActivity : AppCompatActivity(), IntroductionFragment.Listener, CreateWalletListener, LocalAuthListener {

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_flow)
        when {
            sharedPrefsWrapper.onboardingAuthWasInterrupted -> {
                walletServiceLauncher.start()
                loadFragment(LocalAuthFragment())
            }
            sharedPrefsWrapper.onboardingWasInterrupted -> {
                // start wallet service
                walletServiceLauncher.start()
                // clean existing files & restart onboarding
                WalletUtil.clearWalletFiles(walletConfig.getWalletFilesDirPath())
                loadFragment(CreateWalletFragment())
            }
            else -> loadFragment(IntroductionFragment())
        }
    }

    fun navigateToNetworkSelection() = loadFragment(NetworkSelectionFragment(), true)

    private fun loadFragment(fragment: Fragment, isAnimated: Boolean = false) {
        supportFragmentManager
            .beginTransaction()
            .apply {
                if (isAnimated) {
                    setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                }
            }
            .add(R.id.onboarding_fragment_container_1, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Back button should not be functional during onboarding
     */
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun continueToCreateWallet() {
        sharedPrefsWrapper.onboardingStarted = true
        supportFragmentManager.beginTransaction()
            .add(R.id.onboarding_fragment_container_2, CreateWalletFragment())
            .commit()
        removeContainer1Fragment()
    }

    private fun removeContainer1Fragment() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(CreateWallet.removeFragmentDelayDuration)
            launch(Dispatchers.Main) {
                supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_1)?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }
        }
    }

    override fun continueToEnableAuth() {
        (supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_2) as? CreateWalletFragment)?.fadeOutAllViewAnimation()
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .add(R.id.onboarding_fragment_container_1, LocalAuthFragment())
            .commit()

        removeContainer2Fragment()
    }

    private fun removeContainer2Fragment() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(Constants.UI.longDurationMs)
            launch(Dispatchers.Main) {
                supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_2)?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }
        }
    }

    override fun onAuthSuccess() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
