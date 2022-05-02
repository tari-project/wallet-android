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
package com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentWalletRestoringFromSeedWordsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.fragment.restore.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.common.CommonFragment

internal class WalletRestoringFromSeedWordsFragment :
    CommonFragment<FragmentWalletRestoringFromSeedWordsBinding, WalletRestoringFromSeedWordsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWalletRestoringFromSeedWordsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changeOnBackPressed(true)

        val viewModel: WalletRestoringFromSeedWordsViewModel by viewModels()
        bindViewModel(viewModel)

        subscribeUI()

        viewModel.startRestoring()
    }

    private fun subscribeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(recoveryState) { processRecoveryState(it) }
    }

    private fun processNavigation(navigation: WalletRestoringFromSeedWordsNavigation) {
        val router = requireActivity() as WalletRestoreRouter
        when (navigation) {
            WalletRestoringFromSeedWordsNavigation.OnRestoreCompleted -> router.onRestoreCompleted()
            WalletRestoringFromSeedWordsNavigation.OnRestoreFailed -> {
                changeOnBackPressed(false)
                requireActivity().onBackPressed()
            }
        }
    }

    private fun processRecoveryState(state: WalletRestoringFromSeedWordsViewModel.RecoveryState) {
        ui.statusLabel.text = state.status
        ui.progressLabel.text = state.progress
    }

    companion object {

        fun newInstance() = WalletRestoringFromSeedWordsFragment()
    }
}
