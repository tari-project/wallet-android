package com.tari.android.wallet.ui.fragment.tx.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ui.fragment.tx.gif.GIFState.*
import com.tari.android.wallet.ui.presentation.TxNote
import com.tari.android.wallet.ui.presentation.gif.GIF
import com.tari.android.wallet.ui.presentation.gif.GIFRepository
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class GIFViewModel(private val repository: GIFRepository) {
    private val compositeDisposable = CompositeDisposable()
    private val subject = BehaviorSubject.create<String>()
    private val _gifState = MutableLiveData<GIFState>()
    val gifState: LiveData<GIFState> get() = _gifState

    init {
        _gifState.postValue(NoGIFState)
        subject
            .map(TxNote.Companion::fromNote)
            .map { it.gifId ?: "" }
            .switchMap {
                if (it.isEmpty()) Observable.just(NoGIFState)
                else Observable.create { e: ObservableEmitter<GIF> -> retrieveGif(e, it) }
                    .map<GIFState> { state -> SuccessState(state) }
                    .onErrorReturn { ErrorState }
                    .startWith(LoadingState)
                    .subscribeOn(Schedulers.io())
                    .doOnError { e -> Logger.e(e, "Error occurred during gif loading") }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _gifState.postValue(it) }
            .addTo(compositeDisposable)
    }

    private fun retrieveGif(e: ObservableEmitter<GIF>, id: String) {
        try {
            e.onNext(repository.getById(id))
        } catch (exception: Throwable) {
            e.tryOnError(exception)
        }
    }

    fun onNewTxNote(note: String) = subject.onNext(note)

    fun retry() {
        subject.value?.let(subject::onNext)
    }
}