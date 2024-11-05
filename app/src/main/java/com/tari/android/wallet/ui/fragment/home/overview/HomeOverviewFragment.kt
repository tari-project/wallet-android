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
package com.tari.android.wallet.ui.fragment.home.overview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.databinding.FragmentHomeOverviewBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.balanceController.BalanceViewController
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.component.questionMark.QuestionMarkViewModel
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListHomeViewHolder

class HomeOverviewFragment : CommonFragment<FragmentHomeOverviewBinding, HomeOverviewViewModel>() {

    private val networkIndicatorViewModel: ConnectionIndicatorViewModel by viewModels()
    private val questionMarkViewModel: QuestionMarkViewModel by viewModels()

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val adapter = AdapterFactory.generate<CommonViewHolderItem>(TxListHomeViewHolder.getBuilder())

    // This listener is used only to animate the visibility of the scroll depth gradient view.
    private lateinit var balanceViewController: BalanceViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentHomeOverviewBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeOverviewViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.checkPermission()
        setupUI()
        subscribeToViewModel()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {

        observe(refreshBalanceInfo) { updateBalanceInfoUI(it) }

        observe(avatarEmoji) { ui.avatar.text = it }

        observe(emojiMedium) { ui.emptyStateTextView.text = getString(R.string.home_empty_state, it) }

        observe(txList) {
            ui.transactionsRecyclerView.setVisible(it.isNotEmpty())
            ui.viewAllTxsButton.setVisible(it.isNotEmpty())
            ui.emptyState.setVisible(it.isEmpty())
            adapter.update(it)
            adapter.notifyDataSetChanged()
        }

        observeOnLoad(balanceInfo)

        with(transactionRepository) {
            observeOnLoad(requiredConfirmationCount)
            observeOnLoad(listUpdateTrigger)
            observeOnLoad(debouncedList)
        }
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.grantContactsPermission()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() = with(ui) {
        viewAllTxsButton.setOnClickListener { viewModel.navigation.postValue(Navigation.TxListNavigation.HomeTransactionHistory) }
        qrCodeButton.setOnClickListener { QrScannerActivity.startScanner(this@HomeOverviewFragment, QrScannerSource.Home) }
        transactionsRecyclerView.adapter = adapter
        adapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.processItemClick(it) })
        transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fullAvatarContainer.setOnClickListener { viewModel.navigation.postValue(Navigation.AllSettingsNavigation.ToMyProfile) }
    }

    private fun subscribeToViewModel() {
        ui.connectionButton.viewLifecycle = viewLifecycleOwner
        ui.connectionButton.bindViewModel(networkIndicatorViewModel)

        ui.balanceQuestionMark.viewLifecycle = viewLifecycleOwner
        ui.balanceQuestionMark.bindViewModel(questionMarkViewModel)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(requireActivity(), qrDeepLink)
        }
    }

    private fun updateBalanceInfoUI(restart: Boolean) {
        val balanceInfo = viewModel.balanceInfo.value!!

        val availableBalance = WalletConfig.balanceFormatter.format(balanceInfo.availableBalance.tariValue)
        ui.availableBalance.text = availableBalance

        if (restart) {
            ui.balanceDigitContainerView.removeAllViews()
            ui.balanceDecimalsDigitContainerView.removeAllViews()
            createBalanceInfoController(balanceInfo)
            // show digits
            balanceViewController.runStartupAnimation()
        } else {
            if (::balanceViewController.isInitialized) {
                balanceViewController.balanceInfo = balanceInfo
            } else {
                createBalanceInfoController(balanceInfo)
            }
        }
    }

    private fun createBalanceInfoController(info: BalanceInfo) {
        balanceViewController = BalanceViewController(requireContext(), ui.balanceDigitContainerView, ui.balanceDecimalsDigitContainerView, info)
    }
}