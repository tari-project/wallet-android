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

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.purple
import com.tari.android.wallet.R.drawable.*
import com.tari.android.wallet.R.string.store_no_application_to_open_the_link_error
import com.tari.android.wallet.R.string.ttl_store_url
import com.tari.android.wallet.databinding.FragmentStoreBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.store.EventsPropagatingWebViewClient.ExternalSiteOverride
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

class StoreFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    private val webViewStatePublisher = PublishSubject.create<WebViewState>()
    private lateinit var ui: FragmentStoreBinding
    private lateinit var animation: NavigationPanelAnimation
    private var subscription: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStoreBinding.inflate(
        LayoutInflater.from(ContextThemeWrapper(requireContext(), R.style.AppTheme)),
        container,
        false
    ).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        reloadWebViewOnErrorAndConnectedState()
    }

    override fun onDestroyView() {
        ui.webView.destroy()
        animation.dispose()
        subscription?.dispose()
        super.onDestroyView()
    }

    private fun setupUi() {
        animation = NavigationPanelAnimation(ui.controlsView)
        configureWebView()
        ui.progressBar.setColor(color(purple))
        ui.browserBackCtaView.setOnClickListener {
            ui.webView.apply { if (canGoBack()) goBack() }
        }
        ui.browserForwardCtaView.setOnClickListener {
            ui.webView.apply { if (canGoForward()) goForward() }
        }
        ui.shareCtaView.setOnClickListener { shareStoreLink() }
    }

    private fun reloadWebViewOnErrorAndConnectedState() {
        subscription = Observable.combineLatest(
            EventBus.networkConnectionState.publishSubject.distinctUntilChanged(),
            webViewStatePublisher.distinctUntilChanged(),
            BiFunction<NetworkConnectionState, WebViewState, Pair<NetworkConnectionState, WebViewState>>(
                ::Pair
            )
        )
            .filter { it.first == NetworkConnectionState.CONNECTED && it.second.hasError }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ui.webView.reload() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        ui.webView.apply {
            // Needed for product's photos selection
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            webViewClient = EventsPropagatingWebViewClient(
                ExternalSiteOverride(string(ttl_store_url)) {
                    startActivity(Intent(ACTION_VIEW, it))
                }
            ).apply {
                addListener(
                    onPageStarted = { _, _, _ -> updateNavigationState() },
                    onPageCommitVisible = { _, _ -> ui.webView.scrollTo(0, 0) },
                    onPageFinished = { webView, _ ->
                        ui.progressBar.gone()
                        ui.storeTitleTextView.text = webView.title
                    },
                    onReceivedError = { _, _, error ->
                        webViewStatePublisher.onNext(WebViewState(PageLoadingException(error)))
                    }
                )
            }
            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val dy = scrollY - oldScrollY
                this@StoreFragment.animation.processScroll(dy)
            }
        }
        ui.webView.loadUrl(string(ttl_store_url))
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

    private class WebViewState(val error: Exception?) {
        val hasError
            get() = error != null
    }

    private class PageLoadingException(val code: Int, description: String?) :
        IllegalStateException(description) {

        constructor(error: WebResourceError) : this(error.errorCode, error.description?.toString())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PageLoadingException
            return code == other.code
        }

        override fun hashCode(): Int = code

        override fun toString(): String = "PageLoadingException(code=$code, message = $message)"
    }

    private class NavigationPanelAnimation(private val view: View) {
        private var state =
            NavigationPanelAnimationState(TranslationDirection.UP, null)

        fun processScroll(dy: Int) {
            // Ignore weak scrolls
            if (dy >= -5 && dy <= 5) return
            if (dy > 0 && this.state.direction != TranslationDirection.DOWN) {
                state = createState(TranslationDirection.DOWN, to = view.height.toFloat())
            } else if (dy < 0 && this.state.direction != TranslationDirection.UP) {
                state = createState(TranslationDirection.UP, to = 0F)
            }
        }

        private fun createState(direction: TranslationDirection, to: Float) =
            NavigationPanelAnimationState(
                direction,
                ValueAnimator.ofFloat(view.translationY, to).apply {
                    duration = TRANSLATION_DURATION
                    addUpdateListener { view.translationY = it.animatedValue as Float }
                    start()
                })

        fun dispose() {
            this.state.animator?.cancel()
        }

        private companion object {
            private const val TRANSLATION_DURATION = 300L
        }
    }

    private data class NavigationPanelAnimationState(
        val direction: TranslationDirection,
        val animator: Animator?
    )

    private enum class TranslationDirection { UP, DOWN }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = StoreFragment()
    }

}
