package com.tari.android.wallet.ui.screen.contactBook.obsolete.details

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.contact_book_details_delete_button_title
import com.tari.android.wallet.R.string.contact_book_details_delete_contact
import com.tari.android.wallet.R.string.contact_book_details_delete_message
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Close
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Warning
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ContactDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    private val _uiState = MutableStateFlow(ContactDetailsModel.UiState(contact = savedState.getOrThrow<Contact>(PARAMETER_CONTACT)))
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

//        updateYatInfo(uiState.value.contact.yat)
    }

    val ContactDetailsModel.UiState.viewHolderItemList: List<CommonViewHolderItem>
        get() = emptyList()
//            run {
//            val availableActions = this.contact.getContactActions()

//            listOfNotNull(
//                ContactProfileViewHolderItem(
//                    contactDto = contact,
//                    onAddressClick = { showAddressDetailsDialog(it) },
//                ),
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.Send.title),
//                    action = { tariNavigator.navigate(Navigation.ContactBook.ToSendTari(contact)) }
//                ).takeIf { availableActions.contains(ContactAction.Send) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Send) },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.Link.title),
//                    action = { tariNavigator.navigate(Navigation.ContactBook.ToLinkContact(contact)) }
//                ).takeIf { availableActions.contains(ContactAction.Link) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Link) },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(R.string.contact_details_transaction_history),
//                    action = { tariNavigator.navigate(Navigation.ContactBook.ToContactTransactionHistory(contact)) }
//                ).takeIf { contact.getFFIContactInfo() != null },
//                DividerViewHolderItem().takeIf { contact.getFFIContactInfo() != null },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.ToFavorite.title),
//                    action = { toggleFavorite(contact) }
//                ).takeIf { availableActions.contains(ContactAction.ToFavorite) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.ToFavorite) },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.ToUnFavorite.title),
//                    action = { toggleFavorite(contact) }
//                ).takeIf { availableActions.contains(ContactAction.ToUnFavorite) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.ToUnFavorite) },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.Unlink.title),
//                    action = { showUnlinkDialog(contact) }
//                ).takeIf { availableActions.contains(ContactAction.Unlink) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Unlink) },
//
//                SettingsRowViewHolderItem(
//                    title = resourceManager.getString(ContactAction.Delete.title),
//                    style = SettingsRowStyle.Warning,
//                    iconId = R.drawable.tari_empty_drawable,
//                    action = { showDeleteContactDialog(contact) },
//                ).takeIf { availableActions.contains(ContactAction.Delete) },
//                DividerViewHolderItem().takeIf { availableActions.contains(ContactAction.Delete) },
//
//                SettingsTitleViewHolderItem(
//                    title = resourceManager.getString(contact_book_details_connected_wallets),
//                ).takeIf { this.connectedYatWallets.isNotEmpty() },
//                *this.connectedYatWallets.map { connectedWallet ->
//                    listOf(
//                        SettingsRowViewHolderItem(
//                            title = resourceManager.getString(connectedWallet.name!!),
//                            action = { tariNavigator.navigate(Navigation.ContactBook.ToExternalWallet(connectedWallet)) },
//                        ),
//                        DividerViewHolderItem(),
//                    )
//                }.flatten().toTypedArray(),
//
//                ContactTypeViewHolderItem(
//                    type = resourceManager.getString(contact.getTypeName()),
//                    icon = contact.getTypeIcon(),
//                ),
//
//                SpaceVerticalViewHolderItem(20),
//            )
//        }

    private fun toggleFavorite(contactDto: Contact) {
//        launchOnIo {
//            val newContact = contactsRepository.toggleFavorite(contactDto)
//
//            launchOnMain {
//                _uiState.update { it.copy(contact = newContact) }
//            }
//        }
    }

    private fun updateYatInfo(yat: EmojiId?) {
        if (DebugConfig.isYatEnabled && !yat.isNullOrEmpty()) {
            launchOnIo {
                val yatExists = yatAdapter.searchTariYat(yat) != null
                val connectedWallets = yatAdapter.loadConnectedWallets(yat)

                launchOnMain {
                    _uiState.update { it.copy(connectedYatWallets = connectedWallets) }
                    if (!yatExists) {
                        showSimpleDialog(
                            titleRes = R.string.contact_book_details_yat_not_found_dialog_title,
                            descriptionRes = R.string.contact_book_details_yat_not_found_dialog_message,
                        )
                    }
                }
            }
        }
    }

    fun onEditClick() {
//        val contact = uiState.value.contact
//
//        val showYatInput = DebugConfig.isYatEnabled && contact.getPhoneContactInfo() != null
//
//        var saveAction: () -> Boolean = { false }
//
//        val nameModule = InputModule(
//            value = contact.alias,
//            hint = resourceManager.getString(contact_book_add_contact_first_name_hint),
//            isFirst = true,
//            isEnd = !showYatInput,
//            onDoneAction = { saveAction.invoke() },
//        )
//
//        val yatModule = InputModule(
//            value = contact.yat.orEmpty(),
//            hint = resourceManager.getString(contact_book_add_contact_yat_hint),
//            isFirst = false,
//            isEnd = true,
//            onDoneAction = { saveAction.invoke() },
//        ).takeIf { showYatInput }
//
//        val headModule = HeadModule(
//            title = resourceManager.getString(contact_book_details_edit_title),
//            rightButtonTitle = resourceManager.getString(contact_book_add_contact_done_button),
//            rightButtonAction = { saveAction.invoke() },
//        )
//
//        saveAction = {
//            saveDetails(contact, nameModule.value, yatModule?.value.orEmpty())
//            true
//        }
//
//        showInputModalDialog(
//            ModularDialogArgs(
//                modules = listOfNotNull(
//                    headModule,
//                    nameModule,
//                    yatModule.takeIf { it != null },
//                ),
//            )
//        )
    }

    private fun saveDetails(contact: Contact, newName: String, yat: String = "") {
        if (newName.isBlank()) {
            showSimpleDialog(
                titleRes = R.string.contact_details_empty_name_dialog_title,
                descriptionRes = R.string.contact_details_empty_name_dialog_message,
                closeButtonTextRes = R.string.contact_details_empty_name_dialog_button,
            )
        } else {
            launchOnIo {
                val newContact = contactsRepository.updateContactInfo(contact, newName)

                launchOnMain {
//                    if (newContact.yat != contact.yat) {
//                        updateYatInfo(newContact.yat)
//                    }
                    _uiState.update { it.copy(contact = newContact) }
                    hideDialog()
                }
            }
        }
    }

    private fun showUnlinkDialog(contact: Contact) {
//        val mergedDto = uiState.value.contact.contactInfo as MergedContactInfo
//        val walletAddress = mergedDto.ffiContactInfo.walletAddress
//        val name = mergedDto.phoneContactInfo.firstName
//        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_firstLine))
//        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_secondLine, name))
//
//        showModularDialog(
//            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_title)),
//            BodyModule(null, SpannableString(firstLineHtml)),
//            ShortEmojiIdModule(walletAddress),
//            BodyModule(null, SpannableString(secondLineHtml)),
//            ButtonModule(resourceManager.getString(common_confirm), Normal) {
//                launchOnIo {
//                    contactsRepository.unlinkContact(contact)
//                    launchOnMain {
//                        hideDialog()
//                        showUnlinkSuccessDialog(contact)
//                    }
//                }
//            },
//            ButtonModule(resourceManager.getString(common_cancel), Close)
//        )
    }

    private fun showUnlinkSuccessDialog(contact: Contact) {
//        val mergedDto = contact.contactInfo as MergedContactInfo
//        val walletAddress = mergedDto.ffiContactInfo.walletAddress
//        val name = mergedDto.phoneContactInfo.firstName
//        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_firstLine))
//        val secondLineHtml =
//            HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_secondLine, name))
//
//        val modules = listOf(
//            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
//            BodyModule(null, SpannableString(firstLineHtml)),
//            ShortEmojiIdModule(walletAddress),
//            BodyModule(null, SpannableString(secondLineHtml)),
//            ButtonModule(resourceManager.getString(common_close), Close)
//        )
//        showModularDialog(
//            ModularDialogArgs(
//                dialogArgs = DialogArgs(
//                    onDismiss = { tariNavigator.navigate(Navigation.ContactBook.BackToContactBook) }
//                ),
//                modules = modules,
//            )
//        )
    }

    private fun showDeleteContactDialog(contact: Contact) {
        showModularDialog(
            HeadModule(resourceManager.getString(contact_book_details_delete_contact)),
            BodyModule(resourceManager.getString(contact_book_details_delete_message)),
            ButtonModule(resourceManager.getString(contact_book_details_delete_button_title), Warning) {
                launchOnIo {
                    contactsRepository.deleteContact(contact)
                    launchOnMain {
                        hideDialog()
                        tariNavigator.navigate(Navigation.ContactBook.BackToContactBook)
                    }
                }
            },
            ButtonModule(resourceManager.getString(common_close), Close)
        )
    }
}