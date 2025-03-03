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
package com.tari.android.wallet.ui.screen.home.overview

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
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.databinding.FragmentHomeOverviewBinding
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity
import com.tari.android.wallet.util.extension.parcelable

class HomeOverviewFragment_Old : CommonXmlFragment<FragmentHomeOverviewBinding, HomeOverviewViewModel>() {

    private val networkIndicatorViewModel: ConnectionIndicatorViewModel by viewModels() // TODO move it to the new screen? Or not??

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentHomeOverviewBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeOverviewViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.checkPermission()
        setupUI()
        subscribeToViewModel()
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
        qrCodeButton.setOnClickListener { viewModel.onQrScannerClicked(this@HomeOverviewFragment_Old) }
        fullAvatarContainer.setOnClickListener { viewModel.tariNavigator.navigate(Navigation.AllSettings.ToMyProfile) }
    }

    private fun subscribeToViewModel() {
        ui.connectionButton.viewLifecycle = viewLifecycleOwner
        ui.connectionButton.bindViewModel(networkIndicatorViewModel)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(requireActivity(), qrDeepLink)
        }
    }
}