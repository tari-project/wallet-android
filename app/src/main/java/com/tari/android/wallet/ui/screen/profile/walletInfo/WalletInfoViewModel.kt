package com.tari.android.wallet.ui.screen.profile.walletInfo

import android.content.Context
import android.graphics.Bitmap
import com.tari.android.wallet.R
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.data.contacts.model.splitAlias
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.infrastructure.ShareManager
import com.tari.android.wallet.infrastructure.ShareType
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareOptions.ShareOptionsModule
import com.tari.android.wallet.util.ContactUtil
import com.tari.android.wallet.util.QrUtil
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class WalletInfoViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    @Inject
    lateinit var contactUtil: ContactUtil

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        WalletInfoModel.UiState(
            walletAddress = corePrefRepository.walletAddress,
            yat = yatAdapter.connectedYat.orEmpty(),
            alias = corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val shareProfileDeeplink = deeplinkManager.getDeeplinkString(
        DeepLink.UserProfile(
            tariAddress = corePrefRepository.walletAddressBase58.orEmpty(),
            alias = contactUtil.normalizeAlias(uiState.value.alias, corePrefRepository.walletAddress),
        )
    )

    init {
        refreshData()
    }

    fun refreshData() {
        _uiState.update {
            it.copy(
                walletAddress = corePrefRepository.walletAddress,
                yat = yatAdapter.connectedYat,
                alias = corePrefRepository.firstName.orEmpty() + " " + corePrefRepository.lastName.orEmpty(),
            )
        }

        checkYatDisconnected()
    }

    fun openYatOnboarding(context: Context) {
        yatAdapter.openOnboarding(context)
    }

    fun onYatButtonClicked() {
        _uiState.update { it.copy(yatShowing = !it.yatShowing) }
    }

    private fun checkYatDisconnected() {
        launchOnIo {
            val disconnected = yatAdapter.checkYatDisconnected()

            launchOnMain {
                _uiState.update { it.copy(yatDisconnected = disconnected) }
            }
        }
    }

    private fun shareData(type: ShareType) {
        ShareManager.currentInstant?.share(type, shareProfileDeeplink)
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
        if (name.isBlank()) {
            showSimpleDialog(
                titleRes = R.string.wallet_info_empty_name_dialog_title,
                descriptionRes = R.string.wallet_info_empty_name_dialog_message,
                closeButtonTextRes = R.string.wallet_info_empty_name_dialog_button,
            )
        } else {
            corePrefRepository.firstName = splitAlias(name).firstName
            corePrefRepository.lastName = splitAlias(name).lastName
            _uiState.update { it.copy(alias = name) }
            hideDialog()
        }
    }

    fun onAddressDetailsClicked() {
        showAddressDetailsDialog(corePrefRepository.walletAddress)
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

    fun onOpenWalletClicked() {
        tariNavigator.navigate(Navigation.TxList.ToUtxos)
    }
}