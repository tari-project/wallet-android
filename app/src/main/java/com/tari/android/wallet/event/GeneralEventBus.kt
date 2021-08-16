package com.tari.android.wallet.event

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

open class GeneralEventBus {
    val disposables = mutableMapOf<Any, CompositeDisposable>()
    val publishSubject = BehaviorSubject.create<Any>()

    inline fun <reified T : Any> subscribe(subscriber: Any, noinline consumer: (T) -> Unit) {
        val observer = publishSubject.ofType(T::class.java).subscribe(consumer)
        val disposable = disposables[subscriber] ?: CompositeDisposable().apply { disposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribe(subscriber: Any) = disposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun post(event: Any) = publishSubject.onNext(event)
}