package com.tari.android.wallet.ui.fragment.contact_book.details

import android.text.SpannableString
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_cancel
import com.tari.android.wallet.R.string.common_close
import com.tari.android.wallet.R.string.common_confirm
import com.tari.android.wallet.R.string.contact_book_add_contact_done_button
import com.tari.android.wallet.R.string.contact_book_add_contact_first_name_hint
import com.tari.android.wallet.R.string.contact_book_add_contact_surname_hint
import com.tari.android.wallet.R.string.contact_book_add_contact_yat_hint
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_message_firstLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_message_secondLine
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_success_title
import com.tari.android.wallet.R.string.contact_book_contacts_book_unlink_title
import com.tari.android.wallet.R.string.contact_book_details_connected_wallets
import com.tari.android.wallet.R.string.contact_book_details_delete_button_title
import com.tari.android.wallet.R.string.contact_book_details_delete_contact
import com.tari.android.wallet.R.string.contact_book_details_delete_message
import com.tari.android.wallet.R.string.contact_book_details_edit_title
import com.tari.android.wallet.databinding.ViewEmojiIdWithYatSummaryBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Close
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Normal
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Warning
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.dialog.modular.modules.shortEmoji.ShortEmojiIdModule
import com.tari.android.wallet.ui.dialog.modular.modules.yatInput.YatInputModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.contactType.ContactTypeViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile.ContactProfileViewHolderItem
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.yat.YatAdapter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject

class ContactDetailsViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var yatAdapter: YatAdapter

    private var searchingJob: Deferred<YatDto?>? = null
    private var updatingJob: Job? = null

    val contact = MutableLiveData<ContactDto>()

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val initFullEmojiId = SingleLiveEvent<ViewEmojiIdWithYatSummaryBinding>()

    val showFullEmojiId = SingleLiveEvent<Unit>()

    init {
        component.inject(this)

        list.addSource(contact) { updateList() }
    }

    fun initArgs(contactDto: ContactDto) {
        contact.value = contactDto
    }

    private fun updateList() {
        val contact = contact.value ?: return

        updateYatInfo()

        val availableActions = contact.getContactActions()

        val newList = mutableListOf<CommonViewHolderItem>()

        newList.add(ContactProfileViewHolderItem(contact, { showFullEmojiId.postValue(Unit) }) { initFullEmojiId.postValue(it) })

        ContactAction.Send.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(resourceManager.getString(it.title)) {
                    navigation.postValue(Navigation.ContactBookNavigation.ToSendTari(contact))
                }
                newList += DividerViewHolderItem()
            }
        }

        ContactAction.Link.let {
            if (availableActions.contains(it)) {
                newList += (ButtonViewDto(resourceManager.getString(it.title)) {
                    navigation.postValue(Navigation.ContactBookNavigation.ToLinkContact(contact))
                })
                newList += DividerViewHolderItem()
            }
        }

        if (contact.getFFIDto() != null) {
            newList += ButtonViewDto(resourceManager.getString(R.string.contact_details_transaction_history)) {
                navigation.postValue(Navigation.ContactBookNavigation.ToContactTransactionHistory(contact))
            }
            newList += DividerViewHolderItem()
        }

        ContactAction.ToFavorite.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(resourceManager.getString(it.title), iconId = R.drawable.tari_empty_drawable) {
                    this.contact.value = contactsRepository.toggleFavorite(contact)
                }
                newList += DividerViewHolderItem()
            }
        }

        ContactAction.ToUnFavorite.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(resourceManager.getString(it.title), iconId = R.drawable.tari_empty_drawable) {
                    this.contact.value = contactsRepository.toggleFavorite(contact)
                }
                newList += DividerViewHolderItem()
            }
        }

        ContactAction.Unlink.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(resourceManager.getString(it.title)) {
                    showUnlinkDialog()
                }
                newList += DividerViewHolderItem()
            }
        }

        ContactAction.Delete.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(
                    resourceManager.getString(it.title),
                    style = ButtonStyle.Warning,
                    iconId = R.drawable.tari_empty_drawable
                ) {
                    showDeleteContactDialog()
                }
                newList += DividerViewHolderItem()
            }
        }

        contact.getYatDto()?.let {
            val connectedWallets = it.connectedWallets.filter { it.name != null }
            if (connectedWallets.isNotEmpty()) {
                newList.add(SettingsTitleViewHolderItem(resourceManager.getString(contact_book_details_connected_wallets)))
                for (connectedWallet in connectedWallets) {
                    newList += (ButtonViewDto(resourceManager.getString(connectedWallet.name!!)) {
                        navigation.postValue(Navigation.ContactBookNavigation.ToExternalWallet(connectedWallet))
                    })
                    newList += DividerViewHolderItem()
                }
            }
        }

        newList += ContactTypeViewHolderItem(resourceManager.getString(contact.getTypeName()), contact.getTypeIcon())

        newList += SpaceVerticalViewHolderItem(20)

        list.postValue(newList)
    }

    private fun updateYatInfo() = contact.value?.getYatDto()?.let {
        if (updatingJob != null || it.yat.isEmpty()) return@let

        updatingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = yatAdapter.searchAnyYats(it.yat)?.result?.entries
                entries ?: return@launch
                val map = entries.associate { entry -> entry.key to entry.value }
                contactsRepository.updateYatInfo(contact.value!!, map)
                contact.postValue(contactsRepository.getByUuid(contact.value!!.uuid))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun onEditClick() {
        val contact = contact.value!!

        val name = contact.contact.firstName
        val surname = contact.contact.surname
        val phoneDto = contact.getPhoneDto()
        val yatDto = contact.getYatDto()

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(name, resourceManager.getString(contact_book_add_contact_first_name_hint), true, false) { saveAction.invoke() }
        val surnameModule =
            InputModule(surname, resourceManager.getString(contact_book_add_contact_surname_hint), false, phoneDto == null) { saveAction.invoke() }
        val yatModule = phoneDto?.let {
            YatInputModule(this::yatSearchAction, yatDto?.yat.orEmpty(), resourceManager.getString(contact_book_add_contact_yat_hint), false, true) {
                saveAction.invoke()
            }
        }

        val headModule = HeadModule(
            resourceManager.getString(contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(contact_book_add_contact_done_button)
        ) { saveAction.invoke() }

        val moduleList = mutableListOf(headModule, nameModule, surnameModule)
        yatModule?.let { moduleList.add(it) }

        saveAction = {
            saveDetails(nameModule.value, surnameModule.value, yatModule?.value ?: "")
            true
        }

        val args = ModularDialogArgs(DialogArgs(), moduleList)
        _inputDialog.postValue(args)
    }

    private suspend fun yatSearchAction(yat: String): Boolean {
        searchingJob?.cancel()

        if (yat.isEmpty()) return false

        searchingJob = viewModelScope.async(Dispatchers.IO) {
            val entries = yatAdapter.searchTariYats(yat)?.result?.entries?.firstOrNull()
            entries ?: return@async null
            val pubkey = entries.value.address
            val address = walletService.getWalletAddressFromHexString(pubkey) ?: return@async null
            YatDto(yat)
        }
        return searchingJob?.await() != null
    }

    private fun saveDetails(name: String, surname: String = "", yat: String = "") {
        val contact = contact.value!!
        updatingJob = null
        this.contact.value = contactsRepository.updateContactInfo(contact, name, surname, yat)
        dismissDialog.postValue(Unit)
    }

    private fun showUnlinkDialog() {
        val mergedDto = contact.value!!.contact as MergedContactDto
        val walletAddress = mergedDto.ffiContactDto.walletAddress
        val name = mergedDto.phoneContactDto.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_firstLine))
        val secondLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_confirm), Normal) {
                contactsRepository.unlinkContact(contact.value!!)
                dismissDialog.value = Unit
                showUnlinkSuccessDialog()
            },
            ButtonModule(resourceManager.getString(common_cancel), Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showUnlinkSuccessDialog() {
        val mergedDto = contact.value!!.contact as MergedContactDto
        val walletAddress = mergedDto.ffiContactDto.walletAddress
        val name = mergedDto.phoneContactDto.firstName
        val firstLineHtml = HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_firstLine))
        val secondLineHtml =
            HtmlHelper.getSpannedText(resourceManager.getString(contact_book_contacts_book_unlink_success_message_secondLine, name))

        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_contacts_book_unlink_success_title)),
            BodyModule(null, SpannableString(firstLineHtml)),
            ShortEmojiIdModule(walletAddress),
            BodyModule(null, SpannableString(secondLineHtml)),
            ButtonModule(resourceManager.getString(common_close), Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs {
            navigation.value = Navigation.ContactBookNavigation.BackToContactBook()
        }, modules))
    }

    private fun showDeleteContactDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(contact_book_details_delete_contact)),
            BodyModule(resourceManager.getString(contact_book_details_delete_message)),
            ButtonModule(resourceManager.getString(contact_book_details_delete_button_title), Warning) {
                contactsRepository.deleteContact(contact.value!!)
                dismissDialog.postValue(Unit)
                navigation.value = Navigation.ContactBookNavigation.BackToContactBook()
            },
            ButtonModule(resourceManager.getString(common_close), Close)
        )
        modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }
}