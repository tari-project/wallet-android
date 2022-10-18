package com.tari.android.wallet.ffi

fun <T:FFIBase, R> T.runWithDestroy(action: (T) -> R): R {
    val item = action(this)
    destroy()
    return item
}