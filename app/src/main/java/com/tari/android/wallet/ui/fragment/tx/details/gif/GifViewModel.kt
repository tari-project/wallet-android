package com.tari.android.wallet.ui.fragment.tx.details.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GifRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GifViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    @Inject
    lateinit var repository: GifRepository

    private var gifId: String = ""

    private val _gif = MutableLiveData<GifState>()
    val gif: LiveData<GifState> get() = _gif

    val currentState get() = _gif.value!!

    init {
        onGIFFetchRequested()
    }

    fun onGIFFetchRequested(gifId: String) {
        this.gifId = gifId
        onGIFFetchRequested()
    }

    fun onGIFFetchRequested() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _gif.value = GifState() }
            try {
                _gif.postValue(GifState(repository.getById(gifId)))
            } catch (e: Exception) {
                _gif.postValue(GifState(e))
            }
        }
    }
}