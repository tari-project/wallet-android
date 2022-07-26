package com.tari.android.wallet.ui.fragment.send.addNote.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.GiphyKeywordsRepository
import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ThumbnailGIFsViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    @Inject
    lateinit var gifsRepository: GIFRepository

    @Inject
    lateinit var giphyKeywordsRepository: GiphyKeywordsRepository


    private val _state = MutableLiveData<GIFsState>()
    val state: LiveData<GIFsState> get() = _state

    init {
        fetchGIFs()
    }

    private fun fetchGIFs() {
        viewModelScope.launch(Dispatchers.Main) {
            _state.value = GIFsState()
            try {
                val gifs = withContext(Dispatchers.IO) {
                    gifsRepository.getAll(
                        giphyKeywordsRepository.getNext(),
                        THUMBNAIL_REQUEST_LIMIT
                    )
                }
                _state.value = GIFsState(gifs)
            } catch (e: Exception) {
                Logger.e(e, "Error occurred while fetching gifs")
                _state.value = GIFsState(e)
            }
        }
    }

    class GIFsState private constructor(val gifItems: List<GIFItem>?, val error: Exception?) {
        // Loading state
        constructor() : this(null, null)
        constructor(gifItems: List<GIFItem>) : this(gifItems, null)
        constructor(e: Exception) : this(null, e)

        val isError get() = error != null
        val isSuccessful get() = gifItems != null
    }

    companion object {
        const val KEY_GIF = "keygif"
        const val REQUEST_CODE_GIF = 1535
        const val THUMBNAIL_REQUEST_LIMIT = 20
    }
}