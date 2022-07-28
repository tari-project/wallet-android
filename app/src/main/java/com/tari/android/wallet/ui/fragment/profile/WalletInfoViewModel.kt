package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.yat.YatAdapter
import com.tari.android.wallet.yat.YatSharedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletInfoViewModel : CommonViewModel() {
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var yatSharedPrefsRepository: YatSharedRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    private val _emojiId: MutableLiveData<String> = MutableLiveData()
    val emojiId: LiveData<String> = _emojiId

    private val _publicKeyHex: MutableLiveData<String> = MutableLiveData()
    val publicKeyHex: LiveData<String> = _publicKeyHex

    private val _qrDeepLink: MutableLiveData<String> = MutableLiveData()
    val qrDeepLink: LiveData<String> = _qrDeepLink

    private val _yat: MutableLiveData<String> = MutableLiveData()
    val yat: LiveData<String> = _yat

    private val _yatDisconnected: MutableLiveData<Boolean> = MutableLiveData(false)
    val yatDisconnected: LiveData<Boolean> = _yatDisconnected

    private val _isYatForegrounded: MutableLiveData<Boolean> = MutableLiveData(false)
    val isYatForegrounded: LiveData<Boolean> = _isYatForegrounded

    private val _reconnectVisibility: MediatorLiveData<Boolean> = MediatorLiveData()
    val reconnectVisibility: LiveData<Boolean> = _reconnectVisibility

    init {
        component.inject(this)

        _reconnectVisibility.addSource(_yatDisconnected) { updateReconnectVisibility() }
        _reconnectVisibility.addSource(_isYatForegrounded) { updateReconnectVisibility() }

        refreshData()
    }

    fun refreshData() {
        _emojiId.postValue(sharedPrefsWrapper.emojiId)
        _publicKeyHex.postValue(sharedPrefsWrapper.publicKeyHexString)
        val qrCode = deeplinkHandler.getDeeplink(DeepLink.Send(sharedPrefsWrapper.publicKeyHexString.orEmpty()))
        _qrDeepLink.postValue(qrCode)
        _yat.postValue(yatSharedPrefsRepository.connectedYat.orEmpty())
        _yatDisconnected.postValue(yatSharedPrefsRepository.yatWasDisconnected)
        _isYatForegrounded.postValue(false)

        checkEmojiIdConnection()
    }

    fun changeYatVisibility() {
        val newValue = !isYatForegrounded.value!!
        _isYatForegrounded.postValue(!_isYatForegrounded.value!!)
        _emojiId.postValue(if (newValue) yatSharedPrefsRepository.connectedYat else sharedPrefsWrapper.emojiId)
    }

    fun openYatOnboarding(context: Context) {
        yatAdapter.openOnboarding(context)
    }

    private fun checkEmojiIdConnection() {
        val connectedYat = yatSharedPrefsRepository.connectedYat.orEmpty()
        if (connectedYat.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                yatAdapter.searchYats(connectedYat).let {
                    if (it?.status == true) {
                        it.result?.entries?.firstOrNull()?.let { response ->
                            val wasDisconnected = response.value.address.lowercase() != sharedPrefsWrapper.publicKeyHexString.orEmpty().lowercase()
                            yatSharedPrefsRepository.yatWasDisconnected = wasDisconnected
                            _yatDisconnected.postValue(wasDisconnected)
                        }
                    } else {
                        yatSharedPrefsRepository.yatWasDisconnected = true
                        _yatDisconnected.postValue(true)
                    }
                }
            }
        }
    }

    private fun updateReconnectVisibility() {
        _reconnectVisibility.postValue(_isYatForegrounded.value!! && _yatDisconnected.value!!)
    }
}