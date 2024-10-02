package com.tari.android.wallet.ui.fragment.contactBook.data

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.application.walletManager.doOnWalletRunningWithValue
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FFIContactsRepositoryBridge(
    private val contactsRepository: ContactsRepository,
    private val tariWalletServiceConnection: TariWalletServiceConnection,
    private val walletManager: WalletManager,
    private val contactUtil: ContactUtil,
    private val externalScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(this::class.simpleName)

    init {
        externalScope.launch {
            walletManager.walletEvent.collect { event ->
                when (event) {
                    is WalletEvent.Tx.TxReceived,
                    is WalletEvent.Tx.TxReplyReceived,
                    is WalletEvent.Tx.TxFinalized,
                    is WalletEvent.Tx.InboundTxBroadcast,
                    is WalletEvent.Tx.OutboundTxBroadcast,
                    is WalletEvent.Tx.TxMinedUnconfirmed,
                    is WalletEvent.Tx.TxMined,
                    is WalletEvent.Tx.TxFauxMinedUnconfirmed,
                    is WalletEvent.Tx.TxFauxConfirmed,
                    is WalletEvent.Tx.TxCancelled -> contactsRepository.onNewTxReceived()

                    else -> Unit
                }

            }
        }
    }

    suspend fun updateToFFI(contacts: List<ContactDto>) {
        logger.i("Contacts repository event: Saving updates to FFI")

        walletManager.doOnWalletRunning {
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
                        logger.e("Contacts repository event: Error updating contact to FFI: ${error.signature}")
                    }
                }
            }
        }
    }

    suspend fun updateContactsWithFFIContacts(contacts: List<ContactDto>): List<ContactDto> {
        logger.i("Contacts repository event: Loading contacts from FFI and transactions")

        val ffiContacts = loadFFIContacts()

        return contacts
            // update existing contacts
            .map { contactDto ->
                ffiContacts.firstOrNull { it.walletAddress == contactDto.getFFIContactInfo()?.walletAddress }?.let { ffiContact ->
                    when (contactDto.contactInfo) {
                        is FFIContactInfo -> contactDto.copy(contactInfo = ffiContact)
                        is MergedContactInfo -> contactDto.copy(contactInfo = contactDto.contactInfo.copy(ffiContactInfo = ffiContact))
                        else -> contactDto
                    }
                } ?: contactDto
            }
            // add new contacts
            .plus(
                ffiContacts.filter { ffiContact -> contacts.none { it.getFFIContactInfo()?.walletAddress == ffiContact.walletAddress } }
                    .map { ContactDto(it) }
            )
    }

    private suspend fun loadFFIContacts(): List<FFIContactInfo> {
        val walletContacts: List<FFIContactInfo> = try {
            walletManager.doOnWalletRunningWithValue { walletService ->
                walletService.getContacts().items()
                    .map { ffiContact ->
                        FFIContactInfo(
                            walletAddress = TariWalletAddress(ffiContact.getWalletAddress()),
                            alias = ffiContact.getAlias(),
                            isFavorite = ffiContact.getIsFavorite(),
                        )
                    }
            }
        } catch (e: Throwable) {
            logger.e("Contacts repository event: Error getting contacts from FFI: ${e.message}")
            e.printStackTrace()
            emptyList()
        }

        val txContacts: List<FFIContactInfo> = try {
            tariWalletServiceConnection.doOnWalletServiceConnectedWithValue { walletService ->
                listOf(
                    walletService.getCompletedTxs(WalletError()),
                    walletService.getCancelledTxs(WalletError()),
                    walletService.getPendingInboundTxs(WalletError()),
                    walletService.getPendingOutboundTxs(WalletError()),
                ).asSequence().flatten()
                    .filter { !it.tariContact.walletAddress.isUnknownUser() }
                    .filter { !it.isCoinbase }
                    .sortedByDescending { it.timestamp }
                    .map { tx ->
                        // if there is a wallet contact with the same address, use data from it instead of the tx contact
                        val walletContact = walletContacts.firstOrNull { it.walletAddress == tx.tariContact.walletAddress }

                        walletContact?.copy(lastUsedTimeMillis = tx.timestamp.toLong() * 1000L)
                            ?: FFIContactInfo(
                                walletAddress = tx.tariContact.walletAddress,
                                alias = tx.tariContact.alias,
                                isFavorite = tx.tariContact.isFavorite,
                                lastUsedTimeMillis = tx.timestamp.toLong() * 1000L,
                            )
                    }.toList()
            }
        } catch (e: Throwable) {
            logger.e("Contacts repository event: Error getting contacts from transactions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }

        return txContacts // tx ones should be first because of lastUsedTimeMillis
            .plus(walletContacts)
            .distinctBy { it.walletAddress.uniqueIdentifier }
    }

    suspend fun deleteContact(contact: FFIContactInfo) {
        logger.i("Contacts repository event: Deleting contact from FFI")
        walletManager.doOnWalletRunning {
            tariWalletServiceConnection.doOnWalletServiceConnected { service ->
                val error = WalletError()
                service.removeContact(contact.walletAddress, error)
                if (error.code != WalletError.NoError.code) {
                    logger.e("Contacts repository event: Error deleting contact from FFI: ${error.signature}")
                }
            }
        }
    }
}