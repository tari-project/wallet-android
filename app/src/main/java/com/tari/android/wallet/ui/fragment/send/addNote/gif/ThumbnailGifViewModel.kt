package com.tari.android.wallet.ui.fragment.send.addNote.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.GiphyKeywordsRepository
import com.tari.android.wallet.ui.common.gyphy.repository.GifItem
import com.tari.android.wallet.ui.common.gyphy.repository.GifRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ThumbnailGifViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    @Inject
    lateinit var gifsRepository: GifRepository

    @Inject
    lateinit var giphyKeywordsRepository: GiphyKeywordsRepository


    private val _state = MutableLiveData<GifsState>()
    val state: LiveData<GifsState> get() = _state

    init {
        fetchGIFs()
    }

    private fun fetchGIFs() {
        viewModelScope.launch(Dispatchers.Main) {
            _state.value = GifsState()
            try {
                val gifs = withContext(Dispatchers.IO) {
                    gifsRepository.getAll(giphyKeywordsRepository.getNext(), THUMBNAIL_REQUEST_LIMIT)
                }
                _state.value = GifsState(gifs)
            } catch (e: Exception) {
                _state.value = GifsState(e)
            }
        }
    }

    class GifsState private constructor(val gifItems: List<GifItem>?, val error: Exception?) {
        // Loading state
        constructor() : this(null, null)
        constructor(gifItems: List<GifItem>) : this(gifItems, null)
        constructor(e: Exception) : this(null, e)

        val isError get() = error != null
        val isSuccessful get() = gifItems != null
    }

    companion object {
        const val KEY_GIF = "key_gif"
        const val REQUEST_CODE_GIF = 1535
        const val THUMBNAIL_REQUEST_LIMIT = 20
    }
}