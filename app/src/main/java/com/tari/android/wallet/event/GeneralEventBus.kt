package com.tari.android.wallet.event

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

open class GeneralEventBus {
    var disposables = mutableMapOf<Any, CompositeDisposable>()
        private set

    var publishSubject = BehaviorSubject.create<Any>()
        private set

    inline fun <reified T : Any> subscribe(subscriber: Any, noinline consumer: (T) -> Unit) {
        val observer = publishSubject.ofType(T::class.java).subscribe(consumer)
        val disposable = disposables[subscriber] ?: CompositeDisposable().apply { disposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribe(subscriber: Any) = disposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    open fun clear() {
        for (disposable in disposables) disposable.value.dispose()
        disposables.clear()
        publishSubject = BehaviorSubject.create()
        disposables = mutableMapOf()
    }

    fun post(event: Any) = publishSubject.onNext(event)
}