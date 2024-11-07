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

/**
 * Memory-safe find function for FFIIterableBase.
 */
fun <T : FFIIterableBase<I>, I : FFIBase> T.find(predicate: (I) -> Boolean): I? {
    return this.runWithDestroy { iterable ->
        (0 until iterable.getLength()).map { index ->
            val item = iterable.getAt(index)
            if (predicate(item)) {
                return@runWithDestroy item
            }
            item.destroy()
        }
        null
    }
}