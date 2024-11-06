package com.tari.android.wallet.ui.fragment.tx.history

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.data.TransactionRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import javax.inject.Inject

class TransactionHistoryViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }

    // Actualize the contact with the latest data from the repository
    val selectedContact = contactsRepository.getByUuid(savedState.get<ContactDto>(TariNavigator.PARAMETER_CONTACT)!!.uuid)

    var list = MediatorLiveData<List<CommonViewHolderItem>>()

    init {
        collectFlow(contactsRepository.contactList) {
            updateList()
        }

        list.addSource(transactionRepository.list) { updateList() }

        updateList()
    }

    private fun updateList() {
        list.postValue(
            transactionRepository.list.value?.filter {
                if (it is TransactionItem) {
                    it.tx.tariContact.walletAddress == selectedContact.walletAddress
                } else {
                    false
                }
            }.orEmpty().map { (it as TransactionItem).copy(contact = selectedContact) }
        )
    }

    fun onSendTariClick() {
        tariNavigator.navigate(Navigation.TxListNavigation.ToSendTariToUser(selectedContact))
    }

    fun onTransactionClick(tx: Tx) {
        tariNavigator.navigate(Navigation.TxListNavigation.ToTxDetails(tx))
    }
}

