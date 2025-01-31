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
package com.tari.android.wallet.ui.screen.onboarding.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.databinding.ActivityOnboardingFlowBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowModel.Effect
import com.tari.android.wallet.ui.screen.onboarding.createWallet.CreateWalletFragment
import com.tari.android.wallet.ui.screen.onboarding.inroduction.IntroductionFragment
import com.tari.android.wallet.ui.screen.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.screen.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.screen.settings.networkSelection.NetworkSelectionFragment
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.util.extension.safeCastTo
import javax.inject.Inject

/**
 * onBoarding activity class : contain  splash screen and loading sequence.
 *
 * The onboarding flow consists of the following steps:
 * 1. IntroductionFragment
 * 2. CreateWalletFragment
 * 3. LocalAuthFragment
 *
 * @author The Tari Development Team
 */
class OnboardingFlowActivity : CommonActivity<ActivityOnboardingFlowBinding, OnboardingFlowViewModel>(), OnboardingFlowListener {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var walletManager: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityOnboardingFlowBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: OnboardingFlowViewModel by viewModels()
        bindViewModel(viewModel)

        enableEdgeToEdge() // needed for transparent status bar and navigation bar

        setContainerId(R.id.onboarding_fragment_container)

        val paperWalletSeeds = intent.extras?.getStringArray(ARG_SEED_WORDS)?.toList()

        // TODO move this logic to VM. We shouldn't manage scopes inside the activity
        when {
            paperWalletSeeds != null -> {
                walletManager.start(paperWalletSeeds)

                launchOnIo {
                    walletManager.doOnWalletRunning {
                        launchOnMain {
                            loadFragment(WalletRestoringFragment())
                        }
                    }
                }
            }

            corePrefRepository.onboardingAuthWasInterrupted -> {
                walletManager.start()
                loadFragment(LocalAuthFragment())
            }

            corePrefRepository.onboardingWasInterrupted -> {
                // start wallet service
                walletManager.start()
                // clean existing files & restart onboarding
                walletManager.deleteWallet()
                loadFragment(CreateWalletFragment())
            }

            else -> loadFragment(IntroductionFragment())
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                Effect.ResetFlow -> launchOnMain { resetFlow() }
            }
        }

        launchOnIo {
            walletManager.doOnWalletFailed { launchOnMain { resetFlow() } }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container) is NetworkSelectionFragment) {
            super.onBackPressed()
        } else if (!showingIntroductionFragment()) {
            viewModel.safeCastTo<OnboardingFlowViewModel>()?.showResetFlowDialog()
        }
    }

    override fun continueToEnableAuth() {
        supportFragmentManager.findFragmentById(R.id.onboarding_fragment_container)?.safeCastTo<CreateWalletFragment>()?.fadeOutAllViewAnimation()

        loadFragment(
            fragment = LocalAuthFragment(),
            applyTransaction = {
                it.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            },
        )
    }

    override fun continueToCreateWallet() {
        corePrefRepository.onboardingStarted = true

        loadFragment(CreateWalletFragment(), transparentBg = true)
    }

    override fun onAuthSuccess() {
        viewModel.navigateToHome()
        finish()
    }

    override fun navigateToNetworkSelection() {
        loadFragment(
            fragment = NetworkSelectionFragment(),
            applyTransaction = {
                it.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
            },
        )
    }

    override fun resetFlow() {
        walletManager.deleteWallet()
        clearBackStack()
        loadFragment(IntroductionFragment())
    }

    private fun showingIntroductionFragment(): Boolean {
        return supportFragmentManager.fragments.size == 1 && supportFragmentManager.fragments[0] is IntroductionFragment
    }

    private fun loadFragment(
        fragment: Fragment,
        transparentBg: Boolean = false,
        applyTransaction: ((fragmentTransaction: FragmentTransaction) -> FragmentTransaction)? = null,
    ) {
        ui.onboardingFragmentContainer.setBackgroundColor(if (transparentBg) getColor(R.color.transparent) else getColor(R.color.black))

        supportFragmentManager
            .beginTransaction()
            .apply { applyTransaction?.invoke(this) }
            .add(R.id.onboarding_fragment_container, fragment, fragment.javaClass.simpleName)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun clearBackStack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate(supportFragmentManager.getBackStackEntryAt(0).id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    companion object {
        const val ARG_SEED_WORDS = "ARG_SEED_WORDS"
    }
}
