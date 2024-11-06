package com.tari.android.wallet.ffi

fun <T : FFIBase, R> T.runWithDestroy(action: (T) -> R): R {
    val result = action(this)
    destroy()
    return result
}

fun <T : FFIIterableBase<I>, I : FFIBase, R> T.iterateWithDestroy(mapAction: (I) -> R): List<R> {
    return this.runWithDestroy { iterable ->
        (0 until iterable.getLength()).map { index ->
            iterable.getAt(index).runWithDestroy(mapAction)
        }
    }
}