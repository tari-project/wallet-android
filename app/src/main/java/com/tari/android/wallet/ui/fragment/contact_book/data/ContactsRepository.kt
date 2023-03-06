package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.IContact
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.localStorage.ContactSharedPrefRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.localStorage.ContactsList
import io.reactivex.subjects.BehaviorSubject
import org.joda.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    private val contactSharedPrefRepository: ContactSharedPrefRepository
) : CommonViewModel() {
    var publishSubject = BehaviorSubject.create<MutableList<ContactDto>>()

    val ffiBridge = FFIContactsRepositoryBridge()
    val phoneBookRepositoryBridge = PhoneBookRepositoryBridge()

    init {
        val saved = contactSharedPrefRepository.savedContacts.orEmpty()
        if (saved.isEmpty()) {
            val list = (1..20).map { ContactDto.generateContactDto() }.toMutableList()
            publishSubject.onNext(list)
        } else {
            publishSubject.onNext(saved.toMutableList())
        }
    }

    fun updateRecentUsedTime(contact: ContactDto) {
        val existContact = publishSubject.value.orEmpty().firstOrNull { it.uuid == contact.uuid }
        if (existContact == null) {
            withListUpdate { list ->
                list.add(contact)
            }
        }

        updateContact(contact.uuid) {
            it.lastUsedDate = SerializableTime(DateTime.now())
        }
    }

    fun addContact(contact: IContact) {
        withListUpdate {
            val newContact = ContactDto(contact)
            it.add(newContact)
        }
    }

    fun toggleFavorite(contactDto: ContactDto): ContactDto {
        updateContact(contactDto.uuid) {
            it.isFavorite = !it.isFavorite
        }
        return getByUuid(contactDto.uuid)
    }

    fun getByUuid(uuid: String): ContactDto = publishSubject.value!!.first { it.uuid == uuid }

    fun deleteContact(contactDto: ContactDto) {
        updateContact(contactDto.uuid) {
            it.isDeleted = true
        }
    }

    fun linkContacts(ffiContact: ContactDto, phoneContactDto: ContactDto) {
        updateContact(ffiContact.uuid) { it.isDeleted = true }

        updateContact(phoneContactDto.uuid) { it.isDeleted = true }

        val mergedContact = ContactDto(MergedContactDto(ffiContact.contact as FFIContactDto, phoneContactDto.contact as PhoneContactDto))
        withListUpdate { list -> list.add(mergedContact) }
    }

    fun unlinkContact(contact: ContactDto) {
        if (contact.contact is MergedContactDto) {
            withListUpdate { list ->
                list.firstOrNull { it.uuid == contact.uuid }?.let { contact.isDeleted = true }
                list.add(ContactDto(contact.contact.phoneContactDto, contact.isFavorite))
                list.add(ContactDto(contact.contact.ffiContactDto, contact.isFavorite))
            }
        }
    }

    fun updateContactName(contact: ContactDto, newName: String): ContactDto {
        updateContact(contact.uuid) {
            when (val user = it.contact) {
                is FFIContactDto -> user.localAlias = newName
                is PhoneContactDto -> user.name = newName
                is MergedContactDto -> user.phoneContactDto.name = newName
            }
        }
        return getByUuid(contact.uuid)
    }

    private fun updateContact(contactUuid: String, updateAction: (contact: ContactDto) -> Unit) {
        withListUpdate {
            val foundContact = it.firstOrNull { it.uuid == contactUuid }
            foundContact?.let { contact -> updateAction.invoke(contact) }
        }
    }

    private fun withListUpdate(updateAction: (list: MutableList<ContactDto>) -> Unit) {
        val value = publishSubject.value!!
        updateAction.invoke(value)
        contactSharedPrefRepository.savedContacts = ContactsList(value)
        publishSubject.onNext(value)
    }


    inner class FFIContactsRepositoryBridge {
        init {
            subscribeToActions()

            doOnConnectedToWallet { doOnConnected { synchronize() } }
        }

        private fun synchronize() {
            //todo
        }

        private fun subscribeToActions() {
            EventBus.subscribe<Event.Transaction.TxReceived>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxFinalized>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxMined>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { updateRecentUsedTime(it.tx.user) }
            EventBus.subscribe<Event.Transaction.TxCancelled>(this) { updateRecentUsedTime(it.tx.user) }

            EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onFFIContactAddedOrUpdated(it.contactAddress, it.contactAlias) }
            EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onFFIContactRemoved(it.contactAddress) }
        }

        private fun updateRecentUsedTime(user: User) {
            val existContact = publishSubject.value.orEmpty().firstOrNull { it.contact.extractWalletAddress() == user.walletAddress }
            val contact = existContact ?: ContactDto(IContact.generateFromUser(user))
            updateRecentUsedTime(contact)
        }

        private fun onFFIContactAddedOrUpdated(contact: TariWalletAddress, alias: String) {
            if (ffiContactExist(contact)) {
                withFFIContact(contact) {
                    if (it.contact is FFIContactDto) {
                        it.contact.localAlias = alias
                    }
                }
            } else {
                withListUpdate {
                    it.add(ContactDto(IContact.generateFromUser(User(contact))))
                }
            }
        }

        private fun onFFIContactRemoved(contact: TariWalletAddress) {
            withFFIContact(contact) {
                it.isDeleted = true
            }
        }

        private fun ffiContactExist(walletAddress: TariWalletAddress): Boolean =
            publishSubject.value!!.any { it.contact.extractWalletAddress() == walletAddress }

        private fun withFFIContact(walletAddress: TariWalletAddress, updateAction: (contact: ContactDto) -> Unit) {
            withListUpdate {
                val foundContact = it.firstOrNull { it.contact.extractWalletAddress() == walletAddress }
                foundContact?.let { contact -> updateAction.invoke(contact) }
            }
        }
    }

    inner class PhoneBookRepositoryBridge
}