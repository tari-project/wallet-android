package com.tari.android.wallet.ui.fragment.store

import android.graphics.Bitmap
import android.webkit.WebView

interface WebViewEventListener {
    fun onPageStarted(view: WebView, url: String, favicon: Bitmap?)
    fun onPageCommitVisible(view: WebView, url: String)
    fun onPageFinished(view: WebView, url: String)
}
