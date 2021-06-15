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
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.activity.CommonActivity
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.fragment.onboarding.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateWallet
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import yat.android.YatLib
import javax.inject.Inject
import javax.inject.Named

/**
 * onBoarding activity class : contain  splash screen and loading sequence
 *
 * @author The Tari Development Team
 */
internal class OnboardingFlowActivity :
    CommonActivity(),
    IntroductionFragment.Listener,
    CreateWalletFragment.Listener,
    LocalAuthFragment.Listener {

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String

    @Inject
    internal lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var yatUserStorage: YatUserStorage

    private val uiHandler = Handler()
    private lateinit var serviceConnection: TariWalletServiceConnection
    private var walletService: TariWalletService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_flow)
        when {
            sharedPrefsWrapper.onboardingWasInterrupted -> {
                // clean existing files & restart onboarding
                WalletUtil.clearWalletFiles(walletFilesDirPath)
                yatUserStorage.clear()
                yatAdapter.getJWTStorage().clear()
                // start wallet service
                WalletService.start(applicationContext)
                bindWalletService()
                addFragment(R.id.onboarding_fragment_container_2, CreateWalletFragment())
            }
            WalletUtil.walletExists(applicationContext) &&
                    yatUserStorage.get()?.emojiIds?.firstOrNull() == null -> {
                startWalletService()
                bindWalletService()
                addFragment(R.id.onboarding_fragment_container_2, CreateWalletFragment())
            }
            sharedPrefsWrapper.onboardingAuthWasInterrupted -> {
                startWalletService()
                bindWalletService()
                addFragment(R.id.onboarding_fragment_container_1, LocalAuthFragment())
            }
            else -> addFragment(R.id.onboarding_fragment_container_1, IntroductionFragment())
        }
    }

    private fun addFragment(@IdRes id: Int, fragment: Fragment) = supportFragmentManager
        .beginTransaction()
        .add(id, fragment)
        .commitNow()

    // Back button should not be functional during onboarding
    override fun onBackPressed() {}

    private fun startWalletService() = WalletService.start(applicationContext)

    private fun bindWalletService() {
        serviceConnection = ViewModelProvider(this,
            TariWalletServiceConnection.TariWalletServiceConnectionFactory(this)
        ).get()
        serviceConnection.connection.observe(this, {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) {
                walletService = it.service
            }
        })
    }

    override fun continueToCreateWallet() {
        sharedPrefsWrapper.onboardingStarted = true
        bindWalletService()
        val createWalletFragment = CreateWalletFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.onboarding_fragment_container_2, createWalletFragment)
            .commit()
        removeContainer1Fragment()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onWalletCreated() {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container_2)
        if (fragment is CreateWalletFragment) fragment.fadeOutAllViewAnimation()
        if (sharedPrefsWrapper.onboardingAuthSetupCompleted) {
            uiHandler.postDelayed(Constants.UI.mediumDurationMs) {
                this.onAuthSuccess()
                overridePendingTransition(0, R.anim.slide_down)
            }
        } else {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(R.id.onboarding_fragment_container_1, LocalAuthFragment())
                .commit()
            removeContainer2Fragment()
        }
    }

    private fun removeContainer1Fragment() =
        removeFragmentWithDelay(
            R.id.onboarding_fragment_container_1,
            CreateWallet.removeFragmentDelayDuration
        )

    private fun removeContainer2Fragment() =
        removeFragmentWithDelay(R.id.onboarding_fragment_container_2, Constants.UI.longDurationMs)

    private fun removeFragmentWithDelay(@IdRes id: Int, delay: Long) {
        uiHandler.postDelayed(delay) {
            supportFragmentManager.findFragmentById(id)
                ?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
        }
    }

    /**
     * This is to be able to restore Yat data after a wallet restore.
     */
    private fun storeYatDataInWalletDatabase() {
        val yatJWTStorage = yatAdapter.getJWTStorage()
        val service = walletService ?: return
        // get data from local Android storage
        val yatUser = yatUserStorage.get() ?: return
        val accessToken = yatJWTStorage.getAccessToken() ?: return
        val refreshToken = yatJWTStorage.getRefreshToken() ?: return

        val error = WalletError()
        // save yat
        service.setKeyValue(
            WalletService.Companion.KeyValueStorageKeys.YAT_EMOJI_ID,
            yatUser.emojiIds.first().raw,
            error
        )
        Logger.e("YAT STORED :: ${yatUser.emojiIds.first().raw}")
        val yat = service.getKeyValue(WalletService.Companion.KeyValueStorageKeys.YAT_EMOJI_ID, error)
        Logger.e("YAT RESTORED :: $yat")
        // save email
        service.setKeyValue(
            WalletService.Companion.KeyValueStorageKeys.YAT_USER_ALTERNATE_ID,
            yatUser.alternateId,
            error
        )
        // save password
        service.setKeyValue(
            WalletService.Companion.KeyValueStorageKeys.YAT_USER_PASSWORD,
            yatUser.password,
            error
        )
        // save access token
        service.setKeyValue(
            WalletService.Companion.KeyValueStorageKeys.YAT_ACCESS_TOKEN,
            accessToken,
            error
        )
        // save refresh token
        service.setKeyValue(
            WalletService.Companion.KeyValueStorageKeys.YAT_REFRESH_TOKEN,
            refreshToken,
            error
        )
    }

    override fun onAuthSuccess() {
        storeYatDataInWalletDatabase()
        goToHomeActivity()
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

}
