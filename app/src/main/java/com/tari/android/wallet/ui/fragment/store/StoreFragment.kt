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
package com.tari.android.wallet.ui.fragment.store

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R.color.purple
import com.tari.android.wallet.R.drawable.*
import com.tari.android.wallet.R.string.store_no_application_to_open_the_link_error
import com.tari.android.wallet.R.string.ttl_store_url
import com.tari.android.wallet.databinding.FragmentStoreBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.store.EventsPropagatingWebViewClient.ExternalSiteOverride
import com.tari.android.wallet.ui.fragment.store.webView.NavigationPanelAnimation
import com.tari.android.wallet.ui.fragment.store.webView.PageLoadingException
import com.tari.android.wallet.ui.fragment.store.webView.WebViewState

class StoreFragment : CommonFragment<FragmentStoreBinding, StoreViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentStoreBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: StoreViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()
        observeUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        ui.webView.saveState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { ui.webView.restoreState(it) }
    }

    private fun observeUI() = with(viewModel) {
        observe(loadedUrl) { ui.webView.loadUrl(it) }
    }

    override fun onDestroyView() {
        ui.webView.destroy()
        viewModel.animation.dispose()
        viewModel.subscription?.dispose()
        super.onDestroyView()
    }

    private fun setupUi() {
        viewModel.animation = NavigationPanelAnimation(ui.controlsView)
        configureWebView()
        ui.progressBar.setColor(color(purple))
        ui.browserBackCtaView.setOnClickListener { ui.webView.apply { if (canGoBack()) goBack() } }
        ui.browserForwardCtaView.setOnClickListener { ui.webView.apply { if (canGoForward()) goForward() } }
        ui.shareCtaView.setOnClickListener { shareStoreLink() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() = with(ui.webView) {
        // Needed for product's photos selection
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        webViewClient = EventsPropagatingWebViewClient(ExternalSiteOverride(string(ttl_store_url)) {
            startActivity(Intent(ACTION_VIEW, it))
        }).apply {
            addListener(onPageStarted = { _, _, _ -> updateNavigationState() },
                onPageCommitVisible = { _, _ -> ui.webView.scrollTo(0, 0) },
                onPageFinished = { webView, _ ->
                    ui.progressBar.gone()
                    ui.storeTitleTextView.text = webView.title
                },
                onReceivedError = { _, _, error -> viewModel.webViewStatePublisher.onNext(WebViewState(PageLoadingException(error))) })
        }
        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            viewModel.animation.processScroll(dy)
        }
    }

    private fun shareStoreLink() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, ui.webView.url)
            type = "text/plain"
        }

        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            Toast.makeText(requireActivity(), string(store_no_application_to_open_the_link_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun updateNavigationState() {
        val canGoBackIcon = if (ui.webView.canGoBack()) drawable(store_back) else drawable(store_back_disabled)
        ui.browserBackCtaView.setImageDrawable(canGoBackIcon)
        val canGoForwardButton = if (ui.webView.canGoForward()) drawable(store_forward) else drawable(store_forward_disabled)
        ui.browserForwardCtaView.setImageDrawable(canGoForwardButton)
    }
}