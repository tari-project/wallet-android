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
package com.tari.android.wallet.ui.fragment.settings.allSettings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugActivity
import com.tari.android.wallet.ui.fragment.settings.logs.activity.DebugNavigation
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel

class AllSettingsFragment : CommonFragment<FragmentAllSettingsBinding, AllSettingsViewModel>() {

    private val optionsAdapter = AllSettingsOptionAdapter()

    private val biometricAuthenticationViewModel: BiometricAuthenticationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentAllSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AllSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        BiometricAuthenticationViewModel.bindToFragment(biometricAuthenticationViewModel, this)
        viewModel.authenticationViewModel = biometricAuthenticationViewModel

        setupUI()
        observeUI()
    }

    private fun setupUI() {
        ui.optionsList.layoutManager = LinearLayoutManager(requireContext())
        ui.optionsList.adapter = optionsAdapter
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(openYatOnboarding) { yatAdapter.openOnboarding(requireActivity()) }

        observe(allSettingsOptions) { optionsAdapter.update(it) }
    }

    private fun processNavigation(navigation: AllSettingsNavigation) {
        val router = requireActivity() as AllSettingsRouter

        when (navigation) {
            AllSettingsNavigation.ToBugReporting -> DebugActivity.launch(requireContext(), DebugNavigation.BugReport)
            AllSettingsNavigation.ToBackupOnboardingFlow -> router.toBackupOnboardingFlow()
            AllSettingsNavigation.ToAbout -> router.toAbout()
            AllSettingsNavigation.ToBackgroundService -> router.toBackgroundService()
            AllSettingsNavigation.ToBackupSettings -> router.toBackupSettings()
            AllSettingsNavigation.ToBaseNodeSelection -> router.toBaseNodeSelection()
            AllSettingsNavigation.ToDeleteWallet -> router.toDeleteWallet()
            AllSettingsNavigation.ToNetworkSelection -> router.toNetworkSelection()
            AllSettingsNavigation.ToTorBridges -> router.toTorBridges()
            AllSettingsNavigation.ToThemeSelection -> router.toThemeSelection()
        }
    }

    companion object {
        fun newInstance() = AllSettingsFragment()
    }
}

