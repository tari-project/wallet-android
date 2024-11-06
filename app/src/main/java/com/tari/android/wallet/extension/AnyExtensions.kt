package com.tari.android.wallet.extension

/**
 * These extensions can be used to write casts in a chained way.
 * E.g. something.castTo<Other>().doSomething() instead of (something as Other).doSomething()
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.castTo(): T = this as T

/**
 * Simply returning `this as? T` does not work because the Kotlin compiler internally then still just casts it to the other type without checks
 */
inline fun <reified T> Any.safeCastTo(): T? = if (this is T) this else null

/**
 * Takes the receiver and casts it to the type `T` if it is an instance of `T`
 */
inline fun <reified T> Any?.takeIfIs(): T? = this?.safeCastTo<T>()
