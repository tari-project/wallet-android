package com.tari.android.wallet.event

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

open class BehaviorEventBus<Consumer: Any> {
    val disposables = mutableMapOf<Any, CompositeDisposable>()
    val publishSubject = BehaviorSubject.create<Consumer>()

    fun subscribe(subscriber: Any, consumer: (Consumer) -> Unit) {
        val observer = publishSubject.subscribe(consumer)
        val disposable = disposables[subscriber] ?: CompositeDisposable().apply { disposables[subscriber] = this }
        disposable.add(observer)
    }

    inline fun <reified T : Consumer> subscribeOnEvent(subscriber: Any, noinline consumer: (T) -> Unit) {
        val observer = publishSubject.ofType(T::class.java).subscribe(consumer)
        val disposable = disposables[subscriber] ?: CompositeDisposable().apply { disposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribe(subscriber: Any) = disposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun post(event: Consumer) = publishSubject.onNext(event)
}