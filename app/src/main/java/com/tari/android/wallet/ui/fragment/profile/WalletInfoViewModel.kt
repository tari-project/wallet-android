package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
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

    val alias: MutableLiveData<String> = MutableLiveData("")

    private val _yatDisconnected: MutableLiveData<Boolean> = MutableLiveData(false)
    val yatDisconnected: LiveData<Boolean> = _yatDisconnected

    private val _reconnectVisibility: MediatorLiveData<Boolean> = MediatorLiveData()
    val reconnectVisibility: LiveData<Boolean> = _reconnectVisibility

    val sharedText = SingleLiveEvent<String>()

    init {
        component.inject(this)

        _reconnectVisibility.addSource(_yatDisconnected) { updateReconnectVisibility() }

        refreshData()
    }

    fun refreshData() {
        _emojiId.postValue(sharedPrefsWrapper.emojiId)
        _publicKeyHex.postValue(sharedPrefsWrapper.publicKeyHexString)
        val qrCode = deeplinkHandler.getDeeplink(DeepLink.Send(sharedPrefsWrapper.publicKeyHexString.orEmpty()))
        _qrDeepLink.postValue(qrCode)
        _yat.postValue(yatSharedPrefsRepository.connectedYat.orEmpty())
        _yatDisconnected.postValue(yatSharedPrefsRepository.yatWasDisconnected)
        alias.postValue(sharedPrefsWrapper.name.orEmpty() + " " + sharedPrefsWrapper.surname.orEmpty())

        checkEmojiIdConnection()
    }

    fun openYatOnboarding(context: Context) {
        yatAdapter.openOnboarding(context)
    }

    private fun checkEmojiIdConnection() {
        val connectedYat = yatSharedPrefsRepository.connectedYat.orEmpty()
        if (connectedYat.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                yatAdapter.searchTariYats(connectedYat).let {
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
        _reconnectVisibility.postValue(_yatDisconnected.value!!)
    }

    fun shareViaQrCode() {
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = true), listOf(
                HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                ShareQrCodeModule(qrDeepLink.value.orEmpty()),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    fun shareViaLink() {
        sharedText.postValue(qrDeepLink.value.orEmpty())
    }

    fun shareViaNFC() {
        //todo
    }

    fun shareViaBLE() {
        //todo
    }

    fun showEditAliasDialog() {
        val name = sharedPrefsWrapper.name.orEmpty()
        val surname = sharedPrefsWrapper.surname.orEmpty()

        var saveAction: () -> Boolean = { false }

        val nameModule =
            InputModule(name, resourceManager.getString(R.string.contact_book_add_contact_first_name_hint), true, false) { saveAction.invoke() }
        val surnameModule =
            InputModule(surname, resourceManager.getString(R.string.contact_book_add_contact_surname_hint), false, true) { saveAction.invoke() }

        val headModule = HeadModule(
            resourceManager.getString(R.string.wallet_info_alias_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button)
        ) { saveAction.invoke() }

        val moduleList = mutableListOf(headModule, nameModule, surnameModule)
        saveAction = {
            saveDetails(nameModule.value, surnameModule.value)
            true
        }

        val args = ModularDialogArgs(DialogArgs(), moduleList)
        _inputDialog.postValue(args)
    }

    private fun saveDetails(name: String, surname: String) {
        sharedPrefsWrapper.name = name
        sharedPrefsWrapper.surname = surname
        alias.postValue("$name $surname")
        _dismissDialog.postValue(Unit)
    }
}