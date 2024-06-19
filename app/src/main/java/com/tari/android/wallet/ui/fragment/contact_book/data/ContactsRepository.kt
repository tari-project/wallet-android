package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletStateHandler
import com.tari.android.wallet.data.sharedPrefs.contacts.ContactList
import com.tari.android.wallet.data.sharedPrefs.contacts.ContactPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.extension.replaceItem
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactInfo
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.toYatDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.withConnectedWallets
import com.tari.android.wallet.util.ContactUtil
import com.tari.android.wallet.util.nextBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import yat.android.sdk.models.PaymentAddressResponseResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ContactsRepository @Inject constructor(
    context: Context,
    contactUtil: ContactUtil,
    tariWalletServiceConnection: TariWalletServiceConnection,
    walletStateHandler: WalletStateHandler,
    private val contactSharedPrefRepository: ContactPrefRepository,
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
    private val phoneBookRepositoryBridge = PhoneBookRepositoryBridge(
        contactsRepository = this,
        context = context,
    )

    private val _contactList: MutableStateFlow<List<ContactDto>> = MutableStateFlow(contactSharedPrefRepository.savedContacts)
    val contactList = _contactList.asStateFlow()
    val contactListFiltered = _contactList
        .map {
            it.filter { contact ->
                contact.getPhoneContactInfo()?.let { phoneContact ->
                    return@let phoneContact.extractDisplayName().isNotBlank()
                            || phoneContact.firstName.isNotBlank()
                            || phoneContact.lastName.isNotBlank()
                } ?: true
            }
        }
    val currentContactList: List<ContactDto>
        get() = _contactList.value

    private val contactPermission = MutableStateFlow(false)
    val contactPermissionGranted: Boolean
        get() = contactPermission.value

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

    suspend fun updateContactInfo(contactToUpdate: ContactDto, firstName: String, lastName: String, yat: String): ContactDto {
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
                                        yatDto = yat.toYatDto(),
                                    )

                                is PhoneContactInfo ->
                                    contact.contactInfo.copy(
                                        firstName = firstName,
                                        lastName = lastName,
                                        shouldUpdate = true,
                                        yatDto = yat.toYatDto(),
                                    )

                                is MergedContactInfo ->
                                    // TODO why save name 3 times?
                                    contact.contactInfo.copy(
                                        ffiContactInfo = contact.contactInfo.ffiContactInfo.copy(
                                            firstName = firstName,
                                            lastName = lastName,
                                        ),
                                        phoneContactInfo = contact.contactInfo.phoneContactInfo.copy(
                                            firstName = firstName,
                                            lastName = lastName,
                                            shouldUpdate = true,
                                        ),
                                        firstName = firstName,
                                        lastName = lastName,
                                        yatDto = yat.toYatDto(), // TODO extract to ContactDto
                                    )
                            }
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
                .plus(ContactDto(MergedContactInfo(ffiContactInfo, phoneContactInfo)))
        }
    }

    suspend fun unlinkContact(contact: ContactDto) {
        val mergedContact = contact.contactInfo as? MergedContactInfo ?: error("contact must contain MergedContactInfo")

        updateContactList { currentList ->
            currentList.filter { it.uuid != contact.uuid }
                .plus(ContactDto(mergedContact.phoneContactInfo))
                .plus(ContactDto(mergedContact.ffiContactInfo))
        }
    }

    suspend fun deleteContact(contactDto: ContactDto) {
        contactDto.getPhoneContactInfo()?.let { phoneBookRepositoryBridge.deleteFromContactBook(it) }
        contactDto.getFFIContactInfo()?.let { ffiBridge.deleteContact(it) }
        updateContactList { list -> list.filter { it.uuid != contactDto.uuid } }
    }

    suspend fun updateYatInfo(contactDto: ContactDto, connectedWallets: Map<String, PaymentAddressResponseResult>) {
        updateContactList { currentList ->
            currentList
                .replaceItem(
                    condition = { it.uuid == contactDto.uuid },
                    replace = { contact ->
                        contact.copy(
                            contactInfo = when (val contactInfo = contact.contactInfo) { // TODO move yat to ContactDto
                                is FFIContactInfo -> contactInfo.copy(yatDto = contactInfo.yatDto.withConnectedWallets(connectedWallets))
                                is PhoneContactInfo -> contactInfo.copy(yatDto = contactInfo.yatDto.withConnectedWallets(connectedWallets))
                                is MergedContactInfo -> contactInfo.copy(
                                    ffiContactInfo = contactInfo.ffiContactInfo.copy(yatDto = contactInfo.yatDto.withConnectedWallets(connectedWallets)),
                                    phoneContactInfo = contactInfo.phoneContactInfo.copy(
                                        yatDto = contactInfo.yatDto.withConnectedWallets(
                                            connectedWallets
                                        )
                                    ),
                                    yatDto = contactInfo.yatDto.withConnectedWallets(connectedWallets),
                                )
                            }
                        )
                    }
                )

        }
    }

    fun isContactOnline(contact: TariWalletAddress): Boolean {
        // TODO not implemented
        return Random.nextBoolean(0.2)
    }

    internal suspend fun updateRecentUsedTime(contact: ContactDto) {
        updateContactList { currentList ->
            currentList
                .withItemIfNotExists(contact)
                .replaceItem(
                    condition = { it.uuid == contact.uuid },
                    replace = { it.copy(lastUsedDate = SerializableTime(DateTime.now())) }
                )
        }
    }

    private suspend fun updateContactList(update: suspend (List<ContactDto>) -> List<ContactDto>) {
        logger.i("Contacts repository event: Updating contact list")

        val updatedContactList = update(currentContactList)
            .let { phoneBookRepositoryBridge.updateToPhoneBook(it) } // update fields with need shouldUpdate flag and set it false

        ffiBridge.updateToFFI(updatedContactList)

        contactSharedPrefRepository.savedContacts = ContactList(updatedContactList)

        _contactList.update { updatedContactList }
    }

    /**
     * Grant contact permission and refresh contact list if it was not granted before
     */
    suspend fun grantContactPermissionAndRefresh() {
        if (!contactPermission.value) {
            contactPermission.value = true
            updateContactList { phoneBookRepositoryBridge.updateContactListWithPhoneBook(it) }
        }
    }

    private fun ContactDto.contactExists() = currentContactList.any { it.uuid == this.uuid }

    private fun ContactDto.contactExistsByWalletAddress() =
        currentContactList.any { it.contactInfo.extractWalletAddress() == this.contactInfo.extractWalletAddress() }

    private fun List<ContactDto>.withItemIfNotExists(
        contact: ContactDto,
        exists: (ContactDto) -> Boolean = { contact.contactExists() },
    ): List<ContactDto> = if (exists(contact)) this else this.plus(contact)

    fun getContactForTx(tx: Tx): ContactDto = getContactByAddress(tx.tariContact.walletAddress)

    fun getContactByAddress(address: TariWalletAddress): ContactDto =
        currentContactList.firstOrNull { it.getFFIContactInfo()?.walletAddress == address }
            ?: ContactDto(FFIContactInfo(address))

    fun getByUuid(uuid: String): ContactDto = currentContactList.first { it.uuid == uuid }
}