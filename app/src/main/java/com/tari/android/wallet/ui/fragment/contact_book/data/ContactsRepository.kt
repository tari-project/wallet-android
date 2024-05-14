package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.Context
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contact_book.data.localStorage.ContactSharedPrefRepository
import com.tari.android.wallet.util.ContactUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import yat.android.sdk.models.PaymentAddressResponseResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureNanoTime

@Singleton
class ContactsRepository @Inject constructor(
    context: Context,
    contactUtil: ContactUtil,
    tariWalletServiceConnection: TariWalletServiceConnection,
    private val contactSharedPrefRepository: ContactSharedPrefRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(ContactsRepository::class.simpleName)

    private val ffiBridge = FFIContactsRepositoryBridge(
        contactsRepository = this,
        tariWalletServiceConnection = tariWalletServiceConnection,
        contactUtil = contactUtil,
        externalScope = applicationScope,
    )
    private val phoneBookRepositoryBridge = PhoneBookRepositoryBridge(
        contactsRepository = this,
        context = context,
    )

    private val _loadingState = MutableStateFlow(LoadingState())
    val loadingState = _loadingState.asStateFlow()

    private val _contactList = MutableStateFlow(contactSharedPrefRepository.getSavedContacts())
    val contactList = _contactList.asStateFlow()
    val contactListFiltered = _contactList
        .map {
            it.filter { contact ->
                contact.getPhoneDto()?.let { phoneContact ->
                    return@let phoneContact.displayName.isNotBlank()
                            || phoneContact.firstName.isNotBlank()
                            || phoneContact.surname.isNotBlank()
                } ?: true
            }
        }
    val currentContactList: List<ContactDto>
        get() = _contactList.value

    private val contactPermission = MutableStateFlow(false)
    val contactPermissionGranted: Boolean
        get() = contactPermission.value

    suspend fun addContact(contact: ContactDto) {
        if (contact.contactExistsByWalletAddress()) return

        updateContactList { contacts ->
            contacts.add(contact)
        }
    }

    suspend fun toggleFavorite(contactDto: ContactDto): ContactDto {
        updateContact(contactDto.uuid) {
            it.contact.isFavorite = !it.contact.isFavorite
            it.getPhoneDto()?.shouldUpdate = true
        }
        return getByUuid(contactDto.uuid)
    }

    suspend fun updateContactInfo(contact: ContactDto, firstName: String, surname: String, yat: String): ContactDto {
        if (contact.contactExists().not()) addContact(contact)
        updateContact(contact.uuid) {
            when (val user = it.contact) {
                is FFIContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                }

                is PhoneContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                    user.shouldUpdate = true
                    user.saveYat(yat)
                }

                is MergedContactDto -> {
                    user.ffiContactDto.firstName = firstName
                    user.ffiContactDto.surname = surname
                    user.ffiContactDto.isFavorite = contact.contact.isFavorite
                    user.phoneContactDto.firstName = firstName
                    user.phoneContactDto.surname = surname
                    user.phoneContactDto.shouldUpdate = true
                    user.firstName = firstName
                    user.surname = surname
                    user.phoneContactDto.saveYat(yat)
                }
            }
        }
        return getByUuid(contact.uuid)
    }

    suspend fun linkContacts(ffiContact: ContactDto, phoneContactDto: ContactDto) {
        updateContactList { contacts ->
            contacts.remove(getByUuid(ffiContact.uuid))
            contacts.remove(getByUuid(phoneContactDto.uuid))
            contacts.add(ContactDto(MergedContactDto(ffiContact.contact as FFIContactDto, phoneContactDto.contact as PhoneContactDto)))
        }
    }

    suspend fun unlinkContact(contact: ContactDto) {
        (contact.contact as? MergedContactDto)?.let { mergedContact ->
            updateContactList { contacts ->
                contacts.remove(getByUuid(contact.uuid))
                contacts.add(ContactDto(mergedContact.phoneContactDto))
                contacts.add(ContactDto(mergedContact.ffiContactDto))
            }
        }
    }

    suspend fun deleteContact(contactDto: ContactDto) {
        updateContactList { contacts ->
            contactDto.getPhoneDto()?.let { phoneContactDto -> phoneBookRepositoryBridge.deleteFromContactBook(phoneContactDto) }
            contactDto.getFFIDto()?.let { ffiContactDto -> ffiBridge.deleteContact(ffiContactDto) }
            contacts.remove(getByUuid(contactDto.uuid))
        }
    }

    suspend fun updateYatInfo(contactDto: ContactDto, entries: Map<String, PaymentAddressResponseResult>) {
        updateContactList { contacts ->
            val existContact = contacts.firstOrNull { it.uuid == contactDto.uuid }
            existContact?.getYatDto()?.connectedWallets = entries.map { YatDto.ConnectedWallet(it.key, it.value) }
        }
    }

    internal suspend fun updateRecentUsedTime(contact: ContactDto) {
        val existingContact = currentContactList.firstOrNull { it.uuid == contact.uuid }
        if (existingContact == null) {
            updateContactList { contacts ->
                contacts.add(contact)
            }
        }

        updateContact(contact.uuid) {
            it.lastUsedDate = SerializableTime(DateTime.now())
        }
    }

    internal suspend fun doWithLoading(name: String, action: suspend () -> Unit) {
        _loadingState.update { it.copy(isLoading = true, name = name) }
        val time = measureNanoTime { runCatching { action() } } / 1_000_000_000.0
        action()
        logger.i("Action $name took $time seconds")
        _loadingState.update { it.copy(isLoading = false, name = name, time = time) }
    }

    private suspend fun updateContact(contactUuid: String, silently: Boolean = false, updateAction: suspend (contact: ContactDto) -> Unit) {
        updateContactList(silently) { contacts ->
            contacts.firstOrNull { it.uuid == contactUuid }?.let { updateAction(it) }
        }
    }

    internal suspend fun updateContactList(silently: Boolean = false, updateAction: suspend (contacts: MutableList<ContactDto>) -> Unit) {
        val updatedContacts = currentContactList.toMutableList().also { updateAction(it) }.toList()
        _contactList.update { updatedContacts }

        doWithLoading("Updating contact changes to phone and FFI") {
            contactSharedPrefRepository.saveContacts(updatedContacts)
            if (silently.not()) {
                ffiBridge.updateToFFI(updatedContacts)
                phoneBookRepositoryBridge.updateToPhoneBook()
            }
        }
    }

    suspend fun grantContactPermissionAndRefresh() {
        contactPermission.value = true
        phoneBookRepositoryBridge.updateContactListWithPhoneBook()
    }

    private fun ContactDto.contactExists() = currentContactList.any { it.uuid == this.uuid }

    private fun ContactDto.contactExistsByWalletAddress() =
        currentContactList.any { it.contact.extractWalletAddress() == this.contact.extractWalletAddress() }

    fun getContactForTx(tx: Tx): ContactDto = getContactByAddress(tx.tariContact.walletAddress)

    fun getContactByAddress(address: TariWalletAddress): ContactDto =
        currentContactList.firstOrNull { it.getFFIDto()?.walletAddress == address }
            ?: ContactDto(FFIContactDto(address))

    fun getByUuid(uuid: String): ContactDto = currentContactList.first { it.uuid == uuid }

    data class LoadingState(val isLoading: Boolean = false, val name: String = "", val time: Double = 0.0)

    data class PhoneContact(
        val id: String,
        val firstName: String,
        val surname: String,
        val displayName: String,
        val yat: String,
        val emojiId: String,
        val avatar: String,
        val isFavorite: Boolean,
    ) {
        fun toPhoneContactDto(): PhoneContactDto = PhoneContactDto(id, avatar, firstName, surname, yat, isFavorite).apply {
            this.displayName = displayName
        }
    }
}