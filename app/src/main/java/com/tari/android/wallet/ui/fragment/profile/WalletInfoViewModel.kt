package com.tari.android.wallet.ui.fragment.profile

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.yat.YatPrefRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModule
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareOptions.ShareOptionsModule
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.splitAlias
import com.tari.android.wallet.ui.fragment.contactBook.root.ShareViewModel
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType
import com.tari.android.wallet.util.ContactUtil
import com.tari.android.wallet.util.QrUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class WalletInfoViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var yatSharedPrefsRepository: YatPrefRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var contactUtil: ContactUtil

    private val _yatDisconnected: MutableLiveData<Boolean> = MutableLiveData(false) // todo move to UiState
    val yatDisconnected: LiveData<Boolean> = _yatDisconnected

    private val _reconnectVisibility: MediatorLiveData<Boolean> = MediatorLiveData()
    val reconnectVisibility: LiveData<Boolean> = _reconnectVisibility

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        WalletInfoModel.UiState(
            walletAddress = corePrefRepository.walletAddress,
            yat = yatSharedPrefsRepository.connectedYat.orEmpty(),
            alias = corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val shareProfileDeeplink = deeplinkHandler.getDeeplink(
        DeepLink.UserProfile(
            tariAddress = corePrefRepository.walletAddressBase58.orEmpty(),
            alias = contactUtil.normalizeAlias(uiState.value.alias, corePrefRepository.walletAddress),
        )
    )

    init {
        _reconnectVisibility.addSource(_yatDisconnected) { updateReconnectVisibility() }

        refreshData()
    }

    fun refreshData() {
        _uiState.update {
            it.copy(
                walletAddress = corePrefRepository.walletAddress,
                yat = yatSharedPrefsRepository.connectedYat.orEmpty(),
                alias = corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty(),
            )
        }
        _yatDisconnected.postValue(yatSharedPrefsRepository.yatWasDisconnected)

        checkEmojiIdConnection()
    }

    fun openYatOnboarding(context: Context) {
        yatAdapter.openOnboarding(context)
    }

    private fun checkEmojiIdConnection() {
        val connectedYat = yatSharedPrefsRepository.connectedYat.orEmpty()
        if (connectedYat.isNotEmpty()) {
            launchOnIo {
                yatAdapter.searchTariYats(connectedYat).let {
                    if (it?.status == true) {
                        it.result?.entries?.firstOrNull()?.let { response ->
                            val wasDisconnected = response.value.address.lowercase() != corePrefRepository.walletAddressBase58.orEmpty().lowercase()
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

    private fun shareData(type: ShareType) {
        ShareViewModel.currentInstant?.share(type, shareProfileDeeplink)
    }

    fun showEditAliasDialog() {
        val name = (corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty()).trim()

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

    private fun saveDetails(name: String) {
        corePrefRepository.firstName = splitAlias(name).firstName
        corePrefRepository.lastName = splitAlias(name).lastName
        _uiState.update { it.copy(alias = name) }
        hideDialog()
    }

    fun onAddressDetailsClicked() {
        val walletAddress = corePrefRepository.walletAddress
        showModularDialog(
            HeadModule(
                title = resourceManager.getString(R.string.wallet_info_address_details_title),
                rightButtonIcon = R.drawable.vector_common_close,
                rightButtonAction = { hideDialog() },
            ),
            AddressDetailsModule(
                tariWalletAddress = walletAddress,
                copyBase58 = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullBase58,
                    )
                },
                copyEmojis = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullEmojiId,
                    )
                },
            )
        )
    }

    fun onShareAddressClicked() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.wallet_info_share_address_title)),
            BodyModule(resourceManager.getString(R.string.wallet_info_share_address_description)),
            ShareOptionsModule(
                shareQr = { shareData(ShareType.QR_CODE) },
                shareDeeplink = { shareData(ShareType.LINK) },
                shareBle = { shareData(ShareType.BLE) },
            ),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close) { hideDialog() }
        )
    }

    fun getQrCodeBitmap(): Bitmap? = QrUtil.getQrEncodedBitmapOrNull(
        content = shareProfileDeeplink,
        size = resourceManager.getDimenInPx(R.dimen.wallet_info_img_qr_code_size),
    )
}