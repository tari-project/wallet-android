package com.tari.android.wallet.ui.fragment.store.webView

class WebViewState(val error: Exception?) {
    val hasError
        get() = error != null
}