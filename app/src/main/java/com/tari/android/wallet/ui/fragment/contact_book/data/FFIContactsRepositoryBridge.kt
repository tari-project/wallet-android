package com.tari.android.wallet.ui.fragment.contact_book.data

import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FFIContactsRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    private val tariWalletServiceConnection: TariWalletServiceConnection,
    private val contactUtil: ContactUtil,
    private val externalScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    init {
        externalScope.launch {
            tariWalletServiceConnection.doOnWalletRunning {
                tariWalletServiceConnection.doOnWalletServiceConnected {
                    subscribeToActions()
                }
            }
        }
    }

    suspend fun updateToFFI(list: List<ContactDto>) {
        tariWalletServiceConnection.doOnWalletRunning {
            tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                for (item in list.mapNotNull { it.getFFIDto() }) {
                    val error = WalletError()
                    service.updateContact(
                        item.walletAddress,
                        contactUtil.normalizeAlias(item.getAlias(), item.walletAddress),
                        item.isFavorite,
                        error,
                    )
                    if (error.code != WalletError.NoError.code) {
                        logger.i("Error updating contact: ${error.signature}")
                    }
                }
            }
        }
    }

    suspend fun deleteContact(contact: FFIContactDto) {
        contactsRepository.doWithLoading("Deleting contact") {
            tariWalletServiceConnection.doOnWalletRunning {
                tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                    service.updateContact(contact.walletAddress, "", false, WalletError())
                }
            }
        }
    }

    private fun subscribeToActions() {
        EventBus.subscribe<Event.Transaction.TxReceived>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { updateContactChange(it.tx.tariContact) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) { updateContactChange(it.tx.tariContact) }
    }

    private fun updateContactChange(tariContact: TariContact) {
        externalScope.launch {
            contactsRepository.doWithLoading("Updating contact changes to phone and FFI") {
                updateFFIContacts()

                val existingContact = contactsRepository.getContacts()
                    .firstOrNull { it.contact.extractWalletAddress() == tariContact.walletAddress }
                val contact = existingContact ?: ContactDto(FFIContactDto(tariContact.walletAddress, tariContact.alias, tariContact.isFavorite))
                contactsRepository.updateRecentUsedTime(contact)
            }
        }
    }

    private suspend fun updateFFIContacts() {
        contactsRepository.doWithLoading("Updating FFI contacts") {
            try {
                val contacts = FFIWallet.instance!!.getContacts()
                for (contactIndex in 0 until contacts.getLength()) {
                    val actualContact = contacts.getAt(contactIndex)

                    val walletAddress = actualContact.getWalletAddress()
                    val ffiWalletAddress = TariWalletAddress.createWalletAddress(walletAddress.toString(), walletAddress.getEmojiId())
                    val alias = actualContact.getAlias()
                    val isFavorite = actualContact.getIsFavorite()

                    onFFIContactAddedOrUpdated(ffiWalletAddress, alias, isFavorite)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            try {
                tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                    val allTxs = service.getCompletedTxs(WalletError()) + service.getCancelledTxs(WalletError()) +
                            service.getPendingInboundTxs(WalletError()) + service.getPendingOutboundTxs(WalletError())
                    val allUsers = allTxs.map { it.tariContact }.distinctBy { it.walletAddress }
                    for (user in allUsers) {
                        onFFIContactAddedOrUpdated(user.walletAddress, user.alias, user.isFavorite)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun onFFIContactAddedOrUpdated(contact: TariWalletAddress, alias: String, isFavorite: Boolean) {
        if (ffiContactExist(contact)) {
            // TODO turn off updated because of name erasing
//                withFFIContact(contact) {
//                    it.getFFIDto()?.let { ffiContactDto ->
//                        ffiContactDto.setAlias(alias)
//                        ffiContactDto.isFavorite = isFavorite
//                    }
//                }
        } else {
            contactsRepository.updateContactList { contacts ->
                contacts.add(ContactDto(FFIContactDto(contact, alias, isFavorite)))
            }
        }
    }

    private suspend fun withFFIContact(walletAddress: TariWalletAddress, updateAction: (contact: ContactDto) -> Unit) {
        contactsRepository.updateContactList { contacts ->
            contacts.firstOrNull { it.contact.extractWalletAddress() == walletAddress }
                ?.let { contact -> updateAction.invoke(contact) }
        }
    }

    private fun ffiContactExist(walletAddress: TariWalletAddress): Boolean =
        contactsRepository.getContacts().any { it.contact.extractWalletAddress() == walletAddress }
}