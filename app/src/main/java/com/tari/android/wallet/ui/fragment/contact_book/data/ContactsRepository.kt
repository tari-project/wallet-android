package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
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
    val context: Context,
    private val contactSharedPrefRepository: ContactSharedPrefRepository
) : CommonViewModel() {
    var publishSubject = BehaviorSubject.create<MutableList<ContactDto>>()

    val ffiBridge = FFIContactsRepositoryBridge()
    val phoneBookRepositoryBridge = PhoneBookRepositoryBridge()

    init {
        val saved = contactSharedPrefRepository.savedContacts.orEmpty()
        publishSubject.onNext(saved.toMutableList())
        if (saved.isEmpty()) {
//            val list = (1..20).map { ContactDto.generateContactDto() }.toMutableList()
//            publishSubject.onNext(list)
        }
    }

    fun addContact(contact: ContactDto) {
        if (contactExists(contact)) {
            withListUpdate { list ->
                list.firstOrNull { it.uuid == contact.uuid }?.let { list.remove(it) }
            }
        }

        withListUpdate {
            it.add(contact)
        }
    }

    fun toggleFavorite(contactDto: ContactDto): ContactDto {
        updateContact(contactDto.uuid) {
            it.isFavorite = !it.isFavorite
        }
        return getByUuid(contactDto.uuid)
    }

    fun updateContactName(contact: ContactDto, firstName: String, surname: String): ContactDto {
        if (!contactExists(contact)) addContact(contact)
        updateContact(contact.uuid) {
            when (val user = it.contact) {
                is FFIContactDto -> user.localAlias = "$firstName $surname"
                is PhoneContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                }

                is MergedContactDto -> {
                    user.phoneContactDto.firstName = firstName
                    user.phoneContactDto.surname = surname
                }
            }
        }
        return getByUuid(contact.uuid)
    }

    fun linkContacts(ffiContact: ContactDto, phoneContactDto: ContactDto) {

        withListUpdate {
            it.remove(getByUuid(ffiContact.uuid))
            it.remove(getByUuid(phoneContactDto.uuid))
        }

        val mergedContact = ContactDto(MergedContactDto(ffiContact.contact as FFIContactDto, phoneContactDto.contact as PhoneContactDto))
        withListUpdate { list -> list.add(mergedContact) }
    }

    fun unlinkContact(contact: ContactDto) {
        if (contact.contact is MergedContactDto) {
            withListUpdate { list ->
                list.remove(getByUuid(contact.uuid))
                list.add(ContactDto(contact.contact.phoneContactDto, contact.isFavorite))
                list.add(ContactDto(contact.contact.ffiContactDto, contact.isFavorite))
            }
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

    fun deleteContact(contactDto: ContactDto) {
        withListUpdate {
            it.remove(getByUuid(contactDto.uuid))
        }
    }


    private fun updateContact(contactUuid: String, updateAction: (contact: ContactDto) -> Unit) {
        withListUpdate {
            val foundContact = it.firstOrNull { it.uuid == contactUuid }
            foundContact?.let { contact -> updateAction.invoke(contact) }
        }
    }

    private fun getByUuid(uuid: String): ContactDto = publishSubject.value!!.first { it.uuid == uuid }

    private fun contactExists(contact: ContactDto): Boolean = publishSubject.value!!.any { it.uuid == contact.uuid }

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

        fun getContactForTx(tx: Tx): ContactDto {
            val walletAddress = tx.user.walletAddress
            val contact = publishSubject.value!!.firstOrNull {
                it.contact is FFIContactDto && it.contact.walletAddress == walletAddress
                        || it.contact is MergedContactDto && it.contact.ffiContactDto.walletAddress == walletAddress
            }
            return contact ?: ContactDto(FFIContactDto(walletAddress))
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

        private fun onFFIContactRemoved(tariWalletAddress: TariWalletAddress) {
            withListUpdate {
                it.remove(getFFIContact(tariWalletAddress))
            }
        }

        private fun getFFIContact(walletAddress: TariWalletAddress) = publishSubject.value!!.firstOrNull {
            it.contact is FFIContactDto && it.contact.walletAddress == walletAddress
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

    inner class PhoneBookRepositoryBridge {

        fun synchronize() {
            val contacts = getPhoneContacts()

            withListUpdate {
                contacts.forEach { phoneContact ->
                    val existContact = it.firstOrNull {
                        it.contact is PhoneContactDto && it.contact.id == phoneContact.id
                                || it.contact is MergedContactDto && it.contact.phoneContactDto.id == phoneContact.id
                    }
                    if (existContact == null) {
                        it.add(ContactDto(phoneContact.toPhoneContactDto()))
                    } else {
                        if (existContact.contact is MergedContactDto) {
                            existContact.contact.phoneContactDto = phoneContact.toPhoneContactDto()
                        }
                        if (existContact.contact is PhoneContactDto) {
                            existContact.contact.avatar = phoneContact.avatar
                            existContact.contact.firstName = phoneContact.firstName
                            existContact.contact.surname = phoneContact.surname
                        }
                    }
                }
            }

            val c = contacts.size
        }

        fun getPhoneContacts(): MutableList<PhoneContact> {
            val contacts = mutableListOf<PhoneContact>()
            val cr = context.contentResolver
            val cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)!!
            while (cur.moveToNext()) {
                val id = getString(cur, ContactsContract.CommonDataKinds.Phone.NAME_RAW_CONTACT_ID)
                val firstName = getString(cur, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val surname = getString(cur, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val avatar = getString(cur, ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                contacts.add(PhoneContact(id, firstName, surname, avatar))
            }
            cur.close()
            return contacts
        }
    }

    private fun getString(cursor: Cursor, columnName: String): String {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1) return ""
        return cursor.getString(columnIndex).orEmpty()
    }

    data class PhoneContact(
        val id: String,
        val firstName: String,
        val surname: String,
        val avatar: String
    ) {
        fun toPhoneContactDto(): PhoneContactDto = PhoneContactDto(id, firstName, surname, avatar)
    }
}