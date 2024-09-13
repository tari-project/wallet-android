package com.tari.android.wallet.ui.fragment.contactBook.data

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletStateHandler
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.extension.replaceItem
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo
import com.tari.android.wallet.util.ContactUtil
import com.tari.android.wallet.util.EmojiId
import com.tari.android.wallet.util.nextBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ContactsRepository @Inject constructor(
    context: Context,
    contactUtil: ContactUtil,
    tariWalletServiceConnection: TariWalletServiceConnection,
    walletStateHandler: WalletStateHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(ContactsRepository::class.simpleName)

    private val ffiBridge = FFIContactsRepositoryBridge(
        contactsRepository = this,
        tariWalletServiceConnection = tariWalletServiceConnection,
        walletStateHandler = walletStateHandler,
        contactUtil = contactUtil,
        externalScope = applicationScope,
    )
    private val phoneBookBridge = PhoneBookRepositoryBridge(
        contactsRepository = this,
        context = context,
    )
    private val mergedBridge = MergedContactsRepositoryBridge()

    private val _contactList: MutableStateFlow<List<ContactDto>> = MutableStateFlow(emptyList())
    val contactList = _contactList.asStateFlow()

    private val _contactPermission = MutableStateFlow(false)
    val contactPermissionGranted: Boolean
        get() = _contactPermission.value

    init {
        applicationScope.launch {
            refreshContactList()
        }
    }

    /**
     * Add contacts to the contact list if they do not exist already
     */
    suspend fun addContactList(contactsToAdd: List<ContactDto>) {
        updateContactList { currentList -> currentList.plus(contactsToAdd.filter { !it.contactExistsByWalletAddress() }) }
    }

    suspend fun toggleFavorite(contactDto: ContactDto): ContactDto {
        updateContactList { currentList ->
            currentList.replaceItem(
                condition = { it.uuid == contactDto.uuid },
                replace = { contact ->
                    contact.copy(
                        contactInfo = when (val contactInfo = contact.contactInfo) {
                            is FFIContactInfo -> contactInfo.copy(isFavorite = !contactInfo.isFavorite)
                            is PhoneContactInfo -> contactInfo.copy(isFavorite = !contactInfo.isFavorite)
                            is MergedContactInfo -> contactInfo.copy(isFavorite = !contactInfo.isFavorite)
                        }
                    )
                }
            )
        }
        return getByUuid(contactDto.uuid)
    }

    /**
     * Update contact info or add a new contact if it does not exist
     */
    suspend fun updateContactInfo(
        contactToUpdate: ContactDto,
        firstName: String,
        lastName: String,
        yat: EmojiId?,
    ): ContactDto {
        updateContactList { currentList ->
            currentList
                .withItemIfNotExists(contactToUpdate)
                .replaceItem(
                    condition = { it.uuid == contactToUpdate.uuid },
                    replace = { contact ->
                        contact.copy(
                            contactInfo = when (contact.contactInfo) {
                                is FFIContactInfo ->
                                    contact.contactInfo.copy(
                                        firstName = firstName,
                                        lastName = lastName,
                                    )

                                is PhoneContactInfo ->
                                    contact.contactInfo.copy(
                                        firstName = firstName,
                                        lastName = lastName,
                                        shouldUpdate = true,
                                        phoneYat = yat,
                                    )

                                is MergedContactInfo ->
                                    contact.contactInfo.copy(
                                        ffiContactInfo = contact.contactInfo.ffiContactInfo.copy(
                                            firstName = firstName,
                                            lastName = lastName,
                                        ),
                                        phoneContactInfo = contact.contactInfo.phoneContactInfo.copy(
                                            firstName = firstName,
                                            lastName = lastName,
                                            shouldUpdate = true,
                                            phoneYat = yat,
                                        ),
                                    )
                            },
                        )
                    }
                )
        }

        return getByUuid(contactToUpdate.uuid)
    }

    suspend fun linkContacts(ffiContactDto: ContactDto, phoneContactDto: ContactDto) {
        val ffiContactInfo = ffiContactDto.contactInfo as? FFIContactInfo ?: error("ffiContactDto must contain FFIContactInfo")
        val phoneContactInfo = phoneContactDto.contactInfo as? PhoneContactInfo ?: error("phoneContactDto must contain PhoneContactInfo")

        updateContactList { currentList ->
            currentList
                .filter { it.uuid != ffiContactDto.uuid && it.uuid != phoneContactDto.uuid }
                .plus(
                    ContactDto(
                        MergedContactInfo(
                            ffiContactInfo = ffiContactInfo,
                            phoneContactInfo = phoneContactInfo.copy(phoneEmojiId = ffiContactInfo.walletAddress.fullEmojiId, shouldUpdate = true),
                        )
                    )
                )
        }
    }

    suspend fun unlinkContact(contact: ContactDto) {
        val mergedContact = contact.contactInfo as? MergedContactInfo ?: error("contact must contain MergedContactInfo")

        updateContactList { currentList ->
            currentList.filter { it.uuid != contact.uuid }
                .plus(ContactDto(mergedContact.phoneContactInfo.copy(phoneEmojiId = "", shouldUpdate = true)))
                .plus(ContactDto(mergedContact.ffiContactInfo))
        }
    }

    suspend fun deleteContact(contactDto: ContactDto) {
        contactDto.getPhoneContactInfo()?.let { phoneBookBridge.deleteFromContactBook(it) }
        contactDto.getFFIContactInfo()?.let { ffiBridge.deleteContact(it) }
        refreshContactList(contactList.value.filter { it.uuid != contactDto.uuid })
    }

    fun isContactOnline(contact: TariWalletAddress): Boolean {
        // TODO not implemented
        return Random.nextBoolean(0.2)
    }

    fun onNewTxReceived() {
        applicationScope.launch {
            logger.i("Contacts repository event: Updating contact changes to phone and FFI")

            refreshContactList()
        }
    }

    private suspend fun updateContactList(update: suspend (List<ContactDto>) -> List<ContactDto>) {
        logger.i("Contacts repository event: Updating contact list")

        // TODO filter only updated contacts
        val updatedContactList = update(contactList.value)

        phoneBookBridge.updateToPhoneBook(updatedContactList)
        ffiBridge.updateToFFI(updatedContactList)

        refreshContactList(updatedContactList)
    }

    private suspend fun refreshContactList(
        currentContactList: List<ContactDto> = this.contactList.value,
    ) {
        logger.i("Contacts repository event: Refreshing contact list")

        _contactList.update {
            currentContactList
                .let { ffiBridge.updateContactsWithFFIContacts(it) }
                .let { phoneBookBridge.updateContactsWithPhoneBook(it) }
                .let { mergedBridge.updateContactsWithMergedContacts(it) }
        }
    }

    /**
     * Grant contact permission and refresh contact list if it was not granted before
     */
    suspend fun grantContactPermissionAndRefresh() {
        if (!_contactPermission.value) {
            _contactPermission.value = true
            refreshContactList()
        }
    }

    private fun ContactDto.contactExists() = contactList.value.any { it.uuid == this.uuid }

    private fun ContactDto.contactExistsByWalletAddress() =
        contactList.value.any { it.contactInfo.extractWalletAddress() == this.contactInfo.extractWalletAddress() }

    private fun List<ContactDto>.withItemIfNotExists(
        contact: ContactDto,
        exists: (ContactDto) -> Boolean = { contact.contactExists() },
    ): List<ContactDto> = if (exists(contact)) this else this.plus(contact)

    fun getContactForTx(tx: Tx): ContactDto = getContactByAddress(tx.tariContact.walletAddress)

    fun getContactByAddress(address: TariWalletAddress): ContactDto =
        contactList.value.firstOrNull { it.getFFIContactInfo()?.walletAddress == address }
            ?: ContactDto(FFIContactInfo(address))

    fun getByUuid(uuid: String): ContactDto = contactList.value.first { it.uuid == uuid }
}