package com.tari.android.wallet.ui.screen.contactBook.details

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.data.tx.TxListData
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareQr.ShareQrCodeModule
import com.tari.android.wallet.ui.screen.contactBook.details.ContactDetailsFragment.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ContactDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var txRepository: TxRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            contact = savedState.getOrThrow<Contact>(PARAMETER_CONTACT),
            userTxs = txRepository.txs.value.userTxs(savedState.getOrThrow<Contact>(PARAMETER_CONTACT)),
            ticker = networkRepository.currentNetwork.ticker,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(txRepository.txs) { txs -> _uiState.update { it.copy(userTxs = txs.userTxs(uiState.value.contact)) } }
    }

    fun onSendTariClicked() {
        tariNavigator.navigate(Navigation.TxSend.Send(uiState.value.contact))
    }

    fun onRequestTariClicked() {
        tariNavigator.navigate(Navigation.AllSettings.ToRequestTari)
    }

    fun onEmojiCopyClick() {
        tariNavigator.navigate(Navigation.ShareText(uiState.value.contact.walletAddress.fullEmojiId))
    }

    fun onBase58CopyClick() {
        tariNavigator.navigate(Navigation.ShareText(uiState.value.contact.walletAddress.fullBase58))
    }

    fun onAddressDetailsClicked() {
        showAddressDetailsDialog(uiState.value.contact.walletAddress)
    }

    fun onTxClick(txDto: TxDto) {
        tariNavigator.navigate(Navigation.TxList.ToTxDetails(txDto.tx))
    }

    fun onEditAliasClicked() {
        val contact = uiState.value.contact

        val name = contact.alias.orEmpty().trim()

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(
            value = name,
            hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
            isFirst = true,
            isEnd = false,
            onDoneAction = { saveAction.invoke() },
        )

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button),
            rightButtonAction = { saveAction.invoke() },
        )

        saveAction = {
            saveContactName(contact, nameModule.value)
            true
        }

        showInputModalDialog(
            headModule,
            nameModule,
        )
    }

    fun onShareContactClicked() {
        showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = true), listOf(
                    HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                    ShareQrCodeModule(
                        deeplinkManager.getDeeplinkString(
                            DeepLink.UserProfile(
                                tariAddress = uiState.value.contact.walletAddress.fullBase58.orEmpty(),
                                alias = uiState.value.contact.alias.orEmpty(),
                            )
                        )
                    ),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
    }

    fun onDeleteContact() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.common_are_you_sure)),
            BodyModule(resourceManager.getString(R.string.contact_details_delete_contact_dialog_message)),
            ButtonModule(resourceManager.getString(R.string.common_delete), ButtonStyle.Warning) {
                deleteContact()
                hideDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
        )
    }

    private fun saveContactName(contact: Contact, newAlias: String) {
        launchOnMain {
            if (newAlias.isBlank()) {
                showSimpleDialog(
                    titleRes = R.string.contact_details_add_contact_alias_empty_error_title,
                    descriptionRes = R.string.contact_details_add_contact_alias_empty_error_message,
                )
                return@launchOnMain
            }

            _uiState.update { it.copy(contact = contactsRepository.updateContactInfo(contact, newAlias)) }
            hideDialog()
        }
    }

    private fun TxListData.userTxs(contact: Contact): List<TxDto> = this.allTxs
        .filter { it.contact.walletAddress == contact.walletAddress }
        .sortedByDescending { it.tx.timestamp }

    private fun deleteContact() {
        launchOnMain {
            contactsRepository.deleteContact(uiState.value.contact)
            onBackPressed()
        }
    }

    data class UiState(
        val contact: Contact,
        val userTxs: List<TxDto>,
        val ticker: String,
    )
}