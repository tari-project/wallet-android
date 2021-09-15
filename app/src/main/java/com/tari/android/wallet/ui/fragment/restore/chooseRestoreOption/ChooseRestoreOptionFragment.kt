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
package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R.color.all_settings_back_up_status_processing
import com.tari.android.wallet.databinding.FragmentChooseRestoreOptionBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import javax.inject.Inject

internal class ChooseRestoreOptionFragment : CommonFragment<FragmentChooseRestoreOptionBinding, ChooseRestoreOptionViewModel>() {

    @Inject
    internal lateinit var backupManager: BackupManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentChooseRestoreOptionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChooseRestoreOptionViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        observeUI()
    }

    private fun setupUI() = with(ui) {
        backCtaView.setOnThrottledClickListener { requireActivity().onBackPressed() }
        restoreWalletMenuItemProgressView.setColor(color(all_settings_back_up_status_processing))
        restoreWalletMenuItemProgressView.gone()
        restoreWalletCtaView.setOnThrottledClickListener {
            processState(ChooseRestoreOptionState.BeginProgress)
            backupManager.setupStorage(this@ChooseRestoreOptionFragment)
        }
        restoreWithRecoveryPhraseCtaView.setOnThrottledClickListener { processNavigation(ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase) }
    }

    private fun observeUI() = with(viewModel) {
        observe(state) { processState(it) }

        observe(navigation) { processNavigation(it) }
    }

    private fun processState(state: ChooseRestoreOptionState) {
        when (state) {
            ChooseRestoreOptionState.BeginProgress -> beginProgress()
            ChooseRestoreOptionState.EndProgress -> endProgress()
        }
    }

    private fun processNavigation(navigation: ChooseRestoreOptionNavigation) {
        val router = requireActivity() as WalletRestoreRouter
        when (navigation) {
            ChooseRestoreOptionNavigation.ToRestoreInProgress -> router.toRestoreInProgress()
            ChooseRestoreOptionNavigation.ToEnterRestorePassword -> router.toEnterRestorePassword()
            ChooseRestoreOptionNavigation.OnRestoreCompleted -> router.onRestoreCompleted()
            ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase -> router.toRestoreWithRecoveryPhrase()
        }
    }

    private fun beginProgress() = with(ui) {
        blockingBackPressDispatcher.isEnabled = true
        restoreWalletMenuItemProgressView.visible()
        restoreWalletMenuItemArrowImageView.invisible()
        restoreWalletCtaView.isEnabled = false
    }

    private fun endProgress() = with(ui) {
        blockingBackPressDispatcher.isEnabled = false
        restoreWalletMenuItemProgressView.invisible()
        restoreWalletMenuItemArrowImageView.visible()
        restoreWalletCtaView.isEnabled = true
    }

    companion object {
        fun newInstance() = ChooseRestoreOptionFragment()
    }
}

