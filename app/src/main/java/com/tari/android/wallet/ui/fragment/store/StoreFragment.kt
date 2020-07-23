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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.purple
import com.tari.android.wallet.R.drawable.*
import com.tari.android.wallet.R.string.store_no_application_to_open_the_link_error
import com.tari.android.wallet.R.string.ttl_store_url
import com.tari.android.wallet.databinding.FragmentStoreBinding
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.drawable
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.util.UiUtil

class StoreFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    private lateinit var ui: FragmentStoreBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentStoreBinding.inflate(
        LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme)),
        container,
        false
    ).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        configureWebView()
        UiUtil.setProgressBarColor(ui.progressBar, color(purple))
        ui.browserBackCtaView.setOnClickListener {
            ui.webView.apply { if (canGoBack()) goBack() }
        }
        ui.browserForwardCtaView.setOnClickListener {
            ui.webView.apply { if (canGoForward()) goForward() }
        }
        ui.shareCtaView.setOnClickListener { shareStoreLink() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        ui.webView.apply {
            // Needed for product's photos selection
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            webViewClient = EventsPropagatingWebViewClient().apply {
                addListener(
                    onPageStarted = { _, _, _ -> updateNavigationState() },
                    onPageCommitVisible = { _, _ -> ui.scrollView.scrollTo(0, 0) },
                    onPageFinished = { webView, _ ->
                        ui.progressBar.gone()
                        ui.storeTitleTextView.text = webView.title
                    }
                )
            }
            loadUrl(string(ttl_store_url))
        }
    }

    private fun shareStoreLink() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, ui.webView.url)
        shareIntent.type = "text/plain"
        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, null))
        } else {
            Toast.makeText(
                requireActivity(),
                string(store_no_application_to_open_the_link_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateNavigationState() {
        ui.browserBackCtaView.setImageDrawable(
            if (ui.webView.canGoBack()) drawable(store_back)
            else drawable(store_back_disabled)
        )
        ui.browserForwardCtaView.setImageDrawable(
            if (ui.webView.canGoForward()) drawable(store_forward)
            else drawable(store_forward_disabled)
        )
    }

    override fun onDestroyView() {
        ui.webView.destroy()
        super.onDestroyView()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = StoreFragment()
    }

}
