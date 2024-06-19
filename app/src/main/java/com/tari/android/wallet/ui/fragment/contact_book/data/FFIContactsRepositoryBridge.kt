package com.tari.android.wallet.ui.fragment.contact_book.data

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletStateHandler
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactInfo
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FFIContactsRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    private val tariWalletServiceConnection: TariWalletServiceConnection,
    private val walletStateHandler: WalletStateHandler,
    private val contactUtil: ContactUtil,
    private val externalScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    init {
        externalScope.launch {
            walletStateHandler.doOnWalletRunning {
                tariWalletServiceConnection.doOnWalletServiceConnected {
                    subscribeToActions()
                }
            }
        }
    }

    suspend fun updateToFFI(contacts: List<ContactDto>) {
        walletStateHandler.doOnWalletRunning {
            tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                contacts.mapNotNull { it.getFFIContactInfo() }.forEach { ffiContactInfo ->
                    val error = WalletError()
                    service.updateContact(
                        ffiContactInfo.walletAddress,
                        contactUtil.normalizeAlias(ffiContactInfo.getAlias(), ffiContactInfo.walletAddress),
                        ffiContactInfo.isFavorite,
                        error,
                    )
                    if (error.code != WalletError.NoError.code) {
                        logger.i("Error updating contact: ${error.signature}")
                    }
                }
            }
        }
    }

    suspend fun deleteContact(contact: FFIContactInfo) {
        logger.i("Contacts repository event: Deleting contact")
        walletStateHandler.doOnWalletRunning {
            tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                service.updateContact(contact.walletAddress, "", false, WalletError())
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
            logger.i("Contacts repository event: Updating contact changes to phone and FFI")
            updateFFIContacts()

            contactsRepository.updateRecentUsedTime(ContactDto(FFIContactInfo(tariContact.walletAddress, tariContact.alias, tariContact.isFavorite)))
        }
    }

    private suspend fun updateFFIContacts() {
        logger.i("Contacts repository event: Updating FFI contacts")

        val walletContacts: List<ContactDto> = try {
            walletStateHandler.doOnWalletRunningWithValue { walletService ->
                walletService.getContacts().items()
                    .map { ffiContact ->
                        val walletAddress = ffiContact.getWalletAddress()
                        ContactDto(
                            FFIContactInfo(
                                walletAddress = TariWalletAddress.createWalletAddress(walletAddress.toString(), walletAddress.getEmojiId()),
                                alias = ffiContact.getAlias(),
                                isFavorite = ffiContact.getIsFavorite(),
                            )
                        )
                    }
            }
        } catch (e: Throwable) {
            logger.i("Error getting contacts from FFI: ${e.message}")
            e.printStackTrace()
            emptyList()
        }

        val txContacts: List<ContactDto> = try {
            tariWalletServiceConnection.doOnWalletServiceConnectedWithValue { walletService ->
                listOf(
                    walletService.getCompletedTxs(WalletError()),
                    walletService.getCancelledTxs(WalletError()),
                    walletService.getPendingInboundTxs(WalletError()),
                    walletService.getPendingOutboundTxs(WalletError()),
                ).flatten()
                    .map { it.tariContact }
                    .distinctBy { it.walletAddress }
                    .map { tariContact ->
                        ContactDto(
                            FFIContactInfo(
                                walletAddress = tariContact.walletAddress,
                                alias = tariContact.alias,
                                isFavorite = tariContact.isFavorite,
                            )
                        )
                    }
            }
        } catch (e: Throwable) {
            logger.i("Error getting contacts from transactions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }

        contactsRepository.addContactList(walletContacts + txContacts)
    }
}