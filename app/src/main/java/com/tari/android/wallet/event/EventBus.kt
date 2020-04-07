/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.event

import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.tor.TorProxyState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Event bus for the pub/sub model.
 *
 * Subscription example:
 *
 * EventBus.subscribe<EventClass>(this) {
 *      // op.s
 * }
 */
internal object EventBus {

    private val disposables = mutableMapOf<Any, CompositeDisposable>()
    val publishSubject = PublishSubject.create<Any>()

    private val torProxyStateDisposables = mutableMapOf<Any, CompositeDisposable>()
    var torProxyStateSubject = BehaviorSubject.create<TorProxyState>()

    private val walletStateDisposables = mutableMapOf<Any, CompositeDisposable>()
    var walletStateSubject = BehaviorSubject.create<WalletState>()

    private val networkConnectionStateDisposables = mutableMapOf<Any, CompositeDisposable>()
    var networkConnectionStateSubject = BehaviorSubject.create<NetworkConnectionState>()

    inline fun <reified T : Any> subscribe(subscriber: Any, noinline consumer: (T) -> Unit) {
        val observer = publishSubject.ofType(T::class.java).subscribe(consumer)
        val disposable = disposables[subscriber]
            ?: CompositeDisposable().apply { disposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribe(subscriber: Any) = disposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun post(event: Any) = publishSubject.onNext(event)

    fun subscribeToTorProxyState(subscriber: Any, consumer: (TorProxyState) -> Unit) {
        val observer = torProxyStateSubject.ofType(TorProxyState::class.java).subscribe(consumer)
        val disposable = torProxyStateDisposables[subscriber]
            ?: CompositeDisposable().apply { torProxyStateDisposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribeFromTorProxyState(subscriber: Any) = torProxyStateDisposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun postTorProxyState(event: TorProxyState) = torProxyStateSubject.onNext(event)

    fun subscribeToWalletState(subscriber: Any, consumer: (WalletState) -> Unit) {
        val observer = walletStateSubject.ofType(WalletState::class.java).subscribe(consumer)
        val disposable = walletStateDisposables[subscriber]
            ?: CompositeDisposable().apply { walletStateDisposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribeFromWalletState(subscriber: Any) = walletStateDisposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun postWalletState(event: WalletState) = walletStateSubject.onNext(event)

    fun subscribeToNetworkConnectionState(subscriber: Any, consumer: (NetworkConnectionState) -> Unit) {
        val observer = networkConnectionStateSubject.ofType(NetworkConnectionState::class.java).subscribe(consumer)
        val disposable = networkConnectionStateDisposables[subscriber]
            ?: CompositeDisposable().apply { networkConnectionStateDisposables[subscriber] = this }
        disposable.add(observer)
    }

    fun unsubscribeFromNetworkConnectionState(subscriber: Any) = networkConnectionStateDisposables.apply {
        get(subscriber)?.clear()
        remove(subscriber)
    }

    fun postNetworkConnectionState(event: NetworkConnectionState) = networkConnectionStateSubject.onNext(event)

}