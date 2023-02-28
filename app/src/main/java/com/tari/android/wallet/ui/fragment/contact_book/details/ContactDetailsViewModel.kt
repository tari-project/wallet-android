package com.tari.android.wallet.ui.fragment.contact_book.details

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.DividerViewHolder
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

        val favoritesText = if (contact.isFavorite) R.string.contact_book_details_remove_from_favorites else
            R.string.contact_book_details_add_to_favorites

        newList.add(ContactProfileViewHolderItem(contact, availableActions.contains(ContactAction.EditName)) {
            contactsRepository.updateContactName(contact, it)
        })

        if (availableActions.contains(ContactAction.Send)) {
            newList.add(ButtonViewDto(resourceManager.getString(R.string.contact_book_details_send_tari)) {
                navigation.postValue(ContactBookNavigation.ToSendTari(contact))
            })
            newList.add(DividerViewHolderItem())
        }

        if (availableActions.contains(ContactAction.FavoriteToggle)) {
            newList.add(ButtonViewDto(resourceManager.getString(favoritesText), iconId = R.drawable.tari_empty_drawable) {
                this.contact.value = contactsRepository.toggleFavorite(contact)
            })
            newList.add(DividerViewHolderItem())
        }

        if (availableActions.contains(ContactAction.Delete)) {
            newList += ButtonViewDto(resourceManager.getString(R.string.contact_book_details_delete_contact), style = ButtonStyle.Warning, iconId = R.drawable.tari_empty_drawable) {
                contactsRepository.deleteContact(contact)
                _backPressed.postValue(Unit)
            }
            newList += DividerViewHolderItem()
        }

        if (contact.contact is YatContactDto) {
            newList.add(SettingsTitleDto(resourceManager.getString(R.string.contact_book_details_connected_wallets)))
            for (connectedWallet in contact.contact.connectedWallets) {
                newList += (ButtonViewDto(connectedWallet.name) {
                    navigation.postValue(ContactBookNavigation.ToExternalWallet(contact))
                })
                newList += DividerViewHolderItem()
            }
        }

        list.postValue(newList)
    }
}