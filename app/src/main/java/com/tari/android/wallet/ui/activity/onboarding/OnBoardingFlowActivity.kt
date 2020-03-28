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
package com.tari.android.wallet.ui.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.fragment.onboarding.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.onboarding.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.LocalAuthFragment
import com.tari.android.wallet.util.Constants.UI.Auth
import com.tari.android.wallet.util.Constants.UI.CreateWallet
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject
import javax.inject.Named

/**
 * onBoarding activity class : contain  splash screen and loading sequence
 *
 * @author The Tari Development Team
 */
internal class OnboardingFlowActivity : BaseActivity(), IntroductionFragment.Listener,
    CreateWalletFragment.Listener, LocalAuthFragment.Listener {

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String
    @Inject
    internal lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    override val contentViewId = R.layout.activity_onboarding_flow

    private val uiHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            sharedPrefsWrapper.onboardingAuthWasInterrupted -> {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.onboarding_fragment_container_1, LocalAuthFragment())
                    .commitNow()
            }
            sharedPrefsWrapper.onboardingWasInterrupted -> {
                // clean existing files & restart onboarding
                WalletUtil.clearWalletFiles(walletFilesDirPath)
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.onboarding_fragment_container_2, CreateWalletFragment())
                    .commitNow()
            }
            else -> {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.onboarding_fragment_container_1, IntroductionFragment())
                    .commitNow()
            }
        }
    }

    /**
     * Back button should not be functional during onboarding
     */
    override fun onBackPressed() {
        return
    }

    override fun continueToCreateWallet() {
        sharedPrefsWrapper.onboardingStarted = true
        val createWalletFragment = CreateWalletFragment()
        runOnUiThread {
            supportFragmentManager.beginTransaction()
                .add(R.id.onboarding_fragment_container_2, createWalletFragment)
                .commitNow()
            removeContainer1Fragment()
        }
    }

    private fun removeContainer1Fragment() {
        uiHandler.postDelayed({
            val fragment = supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_1)
            fragment?.let {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }, CreateWallet.removeFragmentDelayDuration)
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
    }

    override fun continueToEnableAuth() {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_2)
        if (fragment is CreateWalletFragment) {
            fragment.fadeOutAllViewAnimation()
        }

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.onboarding_fragment_container_1, LocalAuthFragment())
            .commit()

        removeContainer2Fragment()
    }

    private fun removeContainer2Fragment() {
        uiHandler.postDelayed({
            val fragment = supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_2)
            fragment?.let {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }, Auth.removeFragmentDelayDuration)
    }

    override fun onAuthSuccess() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}