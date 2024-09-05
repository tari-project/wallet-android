package com.tari.android.wallet.ui.common.gyphy.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.ui.common.gyphy.presentation.GifState.ErrorState
import com.tari.android.wallet.ui.common.gyphy.presentation.GifState.LoadingState
import com.tari.android.wallet.ui.common.gyphy.presentation.GifState.NoGIFState
import com.tari.android.wallet.ui.common.gyphy.presentation.GifState.SuccessState
import com.tari.android.wallet.ui.common.gyphy.repository.GifRepository
import com.tari.android.wallet.ui.common.gyphy.repository.GifItem
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class GifViewModel(private val repository: GifRepository) {
    private val compositeDisposable = CompositeDisposable()
    private val subject = BehaviorSubject.create<Tx>()
    private val _gifState = MutableLiveData<GifState>()
    val gifState: LiveData<GifState> get() = _gifState

    init {
        _gifState.postValue(NoGIFState)
        subject
            .map(TxNote.Companion::fromTx)
            .map { it.gifId ?: "" }
            .switchMap {
                if (it.isEmpty()) Observable.just(NoGIFState)
                else Observable.create { e: ObservableEmitter<GifItem> -> retrieveGif(e, it) }
                    .map<GifState> { state -> SuccessState(state) }
                    .onErrorReturn { ErrorState }
                    .startWith(LoadingState)
                    .subscribeOn(Schedulers.io())
                    .doOnError { }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _gifState.postValue(it) }
            .addTo(compositeDisposable)
    }

    private fun retrieveGif(e: ObservableEmitter<GifItem>, id: String) {
        try {
            e.onNext(repository.getById(id))
        } catch (exception: Throwable) {
            e.tryOnError(exception)
        }
    }

    fun onNewTx(tx: Tx) = subject.onNext(tx)

    fun retry() {
        subject.value?.let(subject::onNext)
    }
}