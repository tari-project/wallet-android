package com.tari.android.wallet.util.extension

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

inline fun <reified T> Any?.greaterThan(other: T?): Boolean where T : Comparable<T> = if (this is T && other is T) this > other else false

inline fun <T1 : Any, T2 : Any, R : Any> letNotNull(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}