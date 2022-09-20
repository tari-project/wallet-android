package com.tari.android.wallet.ui.fragment.store.webView

import android.webkit.WebResourceError

class PageLoadingException(val code: Int, description: String?) : IllegalStateException(description) {

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