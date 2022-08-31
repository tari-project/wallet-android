package com.tari.android.wallet.ui.fragment.tx.details.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GIFViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    @Inject
    lateinit var repository: GIFRepository

    private var gifId: String = ""

    private val _gif = MutableLiveData<GIFState>()
    val gif: LiveData<GIFState> get() = _gif

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
            withContext(Dispatchers.Main) { _gif.value = GIFState() }
            try {
                _gif.postValue(GIFState(repository.getById(gifId)))
            } catch (e: Exception) {
                Logger.e(e, "Exception was thrown during gif downloading")
                _gif.postValue(GIFState(e))
            }
        }
    }
}