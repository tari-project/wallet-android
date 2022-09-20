package com.tari.android.wallet.ui.fragment.store

import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.store.webView.NavigationPanelAnimation
import com.tari.android.wallet.ui.fragment.store.webView.WebViewState
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

class StoreViewModel() : CommonViewModel() {

    val loadedUrl = SingleLiveEvent<String>()

    val webViewStatePublisher = PublishSubject.create<WebViewState>()
    lateinit var animation: NavigationPanelAnimation
    var subscription: Disposable? = null

    init {
        component.inject(this)

        loadedUrl.postValue(resourceManager.getString(R.string.ttl_store_url))

        subscription = Observable.combineLatest(
            EventBus.networkConnectionState.publishSubject.distinctUntilChanged(),
            webViewStatePublisher.distinctUntilChanged(),
            BiFunction<NetworkConnectionState, WebViewState, Pair<NetworkConnectionState, WebViewState>>(::Pair)
        ).filter { it.first == NetworkConnectionState.CONNECTED && it.second.hasError }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { loadedUrl.call() }
    }
}