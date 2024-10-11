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
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.databinding.ActivityOnboardingFlowBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowModel.Effect
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.onboarding.inroduction.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionFragment
import kotlinx.coroutines.launch
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
class OnboardingFlowActivity : CommonActivity<ActivityOnboardingFlowBinding, CommonViewModel>(), OnboardingFlowListener {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var walletManager: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityOnboardingFlowBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: OnboardingFlowViewModel by viewModels()
        bindViewModel(viewModel)

        setContainerId(R.id.onboarding_fragment_container)

        val paperWalletSeeds = intent.extras?.getStringArray(ARG_SEED_WORDS)?.toList()

        when {
            paperWalletSeeds != null -> {
                walletServiceLauncher.start(paperWalletSeeds)

                lifecycleScope.launch {
                    walletManager.doOnWalletRunning {
                        loadFragment(WalletRestoringFragment())
                    }
                }
            }

            corePrefRepository.onboardingAuthWasInterrupted -> {
                walletServiceLauncher.start()
                loadFragment(LocalAuthFragment())
            }

            corePrefRepository.onboardingWasInterrupted -> {
                // start wallet service
                walletServiceLauncher.start()
                // clean existing files & restart onboarding
                walletManager.deleteWallet()
                loadFragment(CreateWalletFragment())
            }

            else -> loadFragment(IntroductionFragment())
        }

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                Effect.ResetFlow -> {
                    resetFlow()
                }
            }
        }

        lifecycleScope.launch {
            walletManager.doOnWalletFailed {
                resetFlow()
            }
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
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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
            .commit()
    }

    private fun clearBackStack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(supportFragmentManager.getBackStackEntryAt(0).id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    companion object {
        const val ARG_SEED_WORDS = "ARG_SEED_WORDS"
    }
}
