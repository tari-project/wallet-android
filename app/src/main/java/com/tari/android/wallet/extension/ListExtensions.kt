package com.tari.android.wallet.extension

fun <T> MutableList<T>.repopulate(replacement: Iterable<T>) {
    this.clear()
    this.addAll(replacement)
}
