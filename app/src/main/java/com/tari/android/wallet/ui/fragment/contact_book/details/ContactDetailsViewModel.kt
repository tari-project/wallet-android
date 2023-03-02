package com.tari.android.wallet.ui.fragment.contact_book.details

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Close
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle.Warning
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.YatContactDto
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile.ContactProfileViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleDto
import javax.inject.Inject

class ContactDetailsViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val contact = MutableLiveData<ContactDto>()

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val navigation = MutableLiveData<ContactBookNavigation>()

    init {
        component.inject(this)

        list.addSource(contact) { updateList() }
    }

    fun initArgs(contactDto: ContactDto) {
        contact.value = contactDto
    }

    private fun updateList() {
        val contact = contact.value ?: return
        val availableActions = contact.contact.getContactActions()

        val newList = mutableListOf<CommonViewHolderItem>()

        newList.add(ContactProfileViewHolderItem(contact))

        ContactAction.Send.let {
            if (availableActions.contains(it)) {
                newList += ButtonViewDto(resourceManager.getString(it.title)) {
                    navigation.postValue(ContactBookNavigation.ToSendTari(contact))
                }
                newList += DividerViewHolderItem()
            }
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

        ContactAction.Link.let {
            if (availableActions.contains(it)) {
                newList += (ButtonViewDto(resourceManager.getString(it.title)) {
                    navigation.postValue(ContactBookNavigation.ToLinkContact(contact))
                })
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

        if (contact.contact is YatContactDto && availableActions.contains(ContactAction.Yat)) {
            newList.add(SettingsTitleDto(resourceManager.getString(R.string.contact_book_details_connected_wallets)))
            for (connectedWallet in contact.contact.connectedWallets) {
                newList += (ButtonViewDto(connectedWallet.name) {
                    navigation.postValue(ContactBookNavigation.ToExternalWallet(contact))
                })
                newList += DividerViewHolderItem()
            }
        }
        
        newList += SpaceVerticalViewHolderItem(20)

        list.postValue(newList)
    }

    fun onEditClick() {
        val contact = contact.value!!
        val inputModule = InputModule(contact.contact.getAlias(), resourceManager.getString(R.string.contact_book_details_edit_hint))
        val headModule = HeadModule(
            resourceManager.getString(R.string.contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button)
        ) {
            this.contact.value = contactsRepository.updateContactName(contact, inputModule.value)
            _dismissDialog.postValue(Unit)
        }
        val args = ModularDialogArgs(DialogArgs(), listOf(headModule, inputModule))
        _inputDialog.postValue(args)
    }

    private fun showUnlinkDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_message)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), Warning) {
                contactsRepository.unlinkContact(contact.value!!)
                _dismissDialog.postValue(Unit)
                showUnlinkSuccessDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }

    private fun showUnlinkSuccessDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_title)),
            BodyModule(resourceManager.getString(R.string.contact_book_contacts_book_unlink_success_message)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), Warning) { _backPressed.postValue(Unit) },
            ButtonModule(resourceManager.getString(R.string.common_cancel), Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs { _backPressed.postValue(Unit) }, modules))
    }

    private fun showDeleteContactDialog() {
        val modules = listOf(
            HeadModule(resourceManager.getString(R.string.contact_book_details_delete_contact)),
            BodyModule(resourceManager.getString(R.string.contact_book_details_delete_message)),
            ButtonModule(resourceManager.getString(R.string.contact_book_details_delete_message), Warning) {
                contactsRepository.deleteContact(contact.value!!)
                _backPressed.postValue(Unit)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), Close)
        )
        _modularDialog.postValue(ModularDialogArgs(DialogArgs(), modules))
    }
}