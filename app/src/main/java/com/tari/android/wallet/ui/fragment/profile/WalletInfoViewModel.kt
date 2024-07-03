package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.contactBook.root.ShareViewModel
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletInfoViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: CorePrefRepository

    @Inject
    lateinit var yatSharedPrefsRepository: YatPrefRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactUtil: ContactUtil

    private val _emojiId: MutableLiveData<String> = MutableLiveData()
    val emojiId: LiveData<String> = _emojiId

    private val _publicKeyHex: MutableLiveData<String> = MutableLiveData()
    val publicKeyHex: LiveData<String> = _publicKeyHex

    private val _yat: MutableLiveData<String> = MutableLiveData()
    val yat: LiveData<String> = _yat

    val alias: MutableLiveData<String> = MutableLiveData("")

    private val _yatDisconnected: MutableLiveData<Boolean> = MutableLiveData(false)
    val yatDisconnected: LiveData<Boolean> = _yatDisconnected

    private val _reconnectVisibility: MediatorLiveData<Boolean> = MediatorLiveData()
    val reconnectVisibility: LiveData<Boolean> = _reconnectVisibility

    init {
        component.inject(this)

        _reconnectVisibility.addSource(_yatDisconnected) { updateReconnectVisibility() }

        refreshData()
    }

    fun refreshData() {
        _emojiId.postValue(sharedPrefsWrapper.emojiId)
        _publicKeyHex.postValue(sharedPrefsWrapper.publicKeyHexString)
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

    fun shareData(type: ShareType) {
        val walletAddress = TariWalletAddress(
            hexString = sharedPrefsWrapper.publicKeyHexString.orEmpty(),
            emojiId = sharedPrefsWrapper.emojiId.orEmpty(),
        )

        val name = contactUtil.normalizeAlias(alias.value.orEmpty(), walletAddress)
        val hex = sharedPrefsWrapper.publicKeyHexString.orEmpty()

        val deeplink = deeplinkHandler.getDeeplink(DeepLink.UserProfile(hex, name))
        ShareViewModel.currentInstant?.share(type, deeplink)
    }

    fun showEditAliasDialog() {
        val name = (sharedPrefsWrapper.name.orEmpty() + " " + sharedPrefsWrapper.surname.orEmpty()).trim()

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(
            value = name,
            hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
            isFirst = true,
            isEnd = false,
            onDoneAction = { saveAction() },
        )

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.wallet_info_alias_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button),
            rightButtonAction = { saveAction() },
        )

        val moduleList = mutableListOf(headModule, nameModule)
        saveAction = {
            saveDetails(nameModule.value)
            true
        }

        showInputModalDialog(ModularDialogArgs(DialogArgs(), moduleList))
    }

    fun openRequestTari() {
        navigation.postValue(Navigation.AllSettingsNavigation.ToRequestTari)
    }

    private fun saveDetails(name: String) {
        val split = name.split(" ")
        sharedPrefsWrapper.name = split.getOrNull(0).orEmpty().trim()
        sharedPrefsWrapper.surname = split.getOrNull(1).orEmpty().trim()
        alias.postValue(name)
        hideDialog()
    }
}