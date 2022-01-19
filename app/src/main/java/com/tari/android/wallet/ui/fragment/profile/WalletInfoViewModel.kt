package com.tari.android.wallet.ui.fragment.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.yat.YatSharedRepository
import javax.inject.Inject

class WalletInfoViewModel() : CommonViewModel() {
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var yatSharedPrefsRepository: YatSharedRepository

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

    init {
        component.inject(this)

        refreshData()
    }

    fun refreshData() {
        _emojiId.postValue(sharedPrefsWrapper.emojiId)
        _publicKeyHex.postValue(sharedPrefsWrapper.publicKeyHexString)
        val qrCode = WalletUtil.getPublicKeyHexDeepLink(sharedPrefsWrapper.publicKeyHexString!!, networkRepository.currentNetwork!!.network)
        _qrDeepLink.postValue(qrCode)
        _yat.postValue(yatSharedPrefsRepository.connectedYat.orEmpty())
        _yatDisconnected.postValue(false)
    }

    fun changeYatVisibility() {
        val newValue = !isYatForegrounded.value!!
        _isYatForegrounded.postValue(!_isYatForegrounded.value!!)
        _emojiId.postValue(if (newValue) yatSharedPrefsRepository.connectedYat else sharedPrefsWrapper.emojiId)
    }
}