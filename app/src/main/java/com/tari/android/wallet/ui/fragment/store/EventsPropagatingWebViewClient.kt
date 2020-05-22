package com.tari.android.wallet.ui.fragment.store

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

class EventsPropagatingWebViewClient : WebViewClient() {

    private val listeners = mutableListOf<WebViewEventListener>()

    fun addListener(listener: WebViewEventListener) {
        listeners.add(listener)
    }

    fun addListener(
        onPageStarted: (WebView, String, Bitmap?) -> Unit = { _, _, _ -> },
        onPageCommitVisible: (WebView, String) -> Unit = { _, _ -> },
        onPageFinished: (WebView, String) -> Unit = { _, _ -> }
    ): WebViewEventListener {
        return object : WebViewEventListener {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) =
                onPageStarted(view, url, favicon)

            override fun onPageCommitVisible(view: WebView, url: String) =
                onPageCommitVisible(view, url)

            override fun onPageFinished(view: WebView, url: String) = onPageFinished(view, url)
        }.also(this::addListener)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        listeners.forEach { it.onPageStarted(view, url, favicon) }
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        super.onPageCommitVisible(view, url)
        listeners.forEach { it.onPageCommitVisible(view, url) }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        listeners.forEach { it.onPageFinished(view, url) }
    }
}
