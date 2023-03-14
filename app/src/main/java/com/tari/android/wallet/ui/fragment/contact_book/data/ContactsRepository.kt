package com.tari.android.wallet.ui.fragment.contact_book.data

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.localStorage.ContactSharedPrefRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.localStorage.ContactsList
import io.reactivex.subjects.BehaviorSubject
import org.joda.time.DateTime
import yat.android.sdk.models.PaymentAddressResponseResult
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
            it.contact.isFavorite = !it.contact.isFavorite
        }
        return getByUuid(contactDto.uuid)
    }

    fun updateContactInfo(contact: ContactDto, firstName: String, surname: String, yat: String): ContactDto {
        if (!contactExists(contact)) addContact(contact)
        updateContact(contact.uuid) {
            when (val user = it.contact) {
                is YatContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                    user.yat = yat
                    if (yat.isEmpty()) {
                        contact.contact = FFIContactDto(user.walletAddress, firstName, surname, user.isFavorite)
                    }
                }

                is FFIContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                    if (yat.isNotEmpty()) {
                        contact.contact = YatContactDto(user.walletAddress, yat, listOf(), user.getAlias()).apply {
                            isFavorite = contact.contact.isFavorite
                        }
                    }
                }

                is PhoneContactDto -> {
                    user.firstName = firstName
                    user.surname = surname
                }

                is MergedContactDto -> {
                    user.phoneContactDto.firstName = firstName
                    user.phoneContactDto.surname = surname
                    val yatContactDto = contact.getYatDto()
                    if (yatContactDto != null) {
                        yatContactDto.yat = yat
                    } else if (yat.isNotEmpty()) {
                        user.ffiContactDto = YatContactDto(user.ffiContactDto.walletAddress, yat, listOf(), user.ffiContactDto.getAlias())
                    }
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
        (contact.contact as? MergedContactDto)?.let { mergedContact ->
            withListUpdate { list ->
                list.remove(getByUuid(contact.uuid))
                list.add(ContactDto(mergedContact.phoneContactDto))
                list.add(ContactDto(mergedContact.ffiContactDto))
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

    fun getByUuid(uuid: String): ContactDto = publishSubject.value!!.first { it.uuid == uuid }

    private fun contactExists(contact: ContactDto): Boolean = publishSubject.value!!.any { it.uuid == contact.uuid }

    private fun withListUpdate(updateAction: (list: MutableList<ContactDto>) -> Unit) {
        val value = publishSubject.value!!
        updateAction.invoke(value)
        contactSharedPrefRepository.savedContacts = ContactsList(value)
        publishSubject.onNext(value)
    }


    inner class FFIContactsRepositoryBridge {
        init {
            doOnConnectedToWallet { doOnConnected { subscribeToActions() } }
        }

        fun getContactForTx(tx: Tx): ContactDto = getContactByAdress(tx.tariContact.walletAddress)

        fun getContactByAdress(address: TariWalletAddress): ContactDto =
            publishSubject.value!!.firstOrNull { it.getFFIDto()?.walletAddress == address } ?: ContactDto(FFIContactDto(address))

        private fun subscribeToActions() {
            EventBus.subscribe<Event.Transaction.TxReceived>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxFinalized>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxMined>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { updateRecentUsedTime(it.tx.tariContact) }
            EventBus.subscribe<Event.Transaction.TxCancelled>(this) { updateRecentUsedTime(it.tx.tariContact) }
        }

        private fun updateRecentUsedTime(tariContact: TariContact) {
            val contacts = FFIWallet.instance!!.getContacts()
            for (contactIndex in 0 until contacts.getLength()) {
                val actualContact = contacts.getAt(contactIndex)

                val walletAddress = actualContact.getWalletAddress()
                val ffiWalletAddress = TariWalletAddress(walletAddress.toString(), walletAddress.getEmojiId())
                val alias = actualContact.getAlias()
                val isFavorite = actualContact.getIsFavorite()

                onFFIContactAddedOrUpdated(ffiWalletAddress, alias, isFavorite)
            }


            val existContact = publishSubject.value.orEmpty().firstOrNull { it.contact.extractWalletAddress() == tariContact.walletAddress }
            val contact = existContact ?: ContactDto(FFIContactDto(tariContact.walletAddress))
            updateRecentUsedTime(contact)
        }

        private fun onFFIContactAddedOrUpdated(contact: TariWalletAddress, alias: String, isFavorite: Boolean) {
            if (ffiContactExist(contact)) {
                withFFIContact(contact) {
                    it.getFFIDto()?.let { ffiContactDto ->
                        ffiContactDto.setAlias(alias)
                        ffiContactDto.isFavorite = isFavorite
                    }
                }
            } else {
                withListUpdate {
                    it.add(ContactDto(FFIContactDto(contact, alias, isFavorite)))
                }
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

        fun updateYatInfo(contactDto: ContactDto, entries: Map<String, PaymentAddressResponseResult>) {
            withListUpdate {
                val existContact = it.firstOrNull { it.uuid == contactDto.uuid }
                existContact?.getYatDto()?.connectedWallets = entries.map { YatContactDto.ConnectedWallet(it.key, it.value) }
            }
        }
    }

    inner class PhoneBookRepositoryBridge {

        fun synchronize() {
            val contacts = getPhoneContacts()

            withListUpdate {
                contacts.forEach { phoneContact ->
                    val existContact = it.firstOrNull { it.getPhoneDto()?.id == phoneContact.id }
                    if (existContact == null) {
                        it.add(ContactDto(phoneContact.toPhoneContactDto()))
                    } else {
                        existContact.getPhoneDto()?.let {
                            it.avatar = phoneContact.avatar
                            it.firstName = phoneContact.firstName
                            it.surname = phoneContact.surname
                        }
                    }
                }
            }
        }

        fun getPhoneContacts(): MutableList<PhoneContact> {
            val contacts = mutableListOf<PhoneContact>()
            val cr = context.contentResolver

            val cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)!!
            while (cur.moveToNext()) {
                val id = getString(cur, ContactsContract.CommonDataKinds.Phone.NAME_RAW_CONTACT_ID)
                val avatar = getString(cur, ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val isFavorite = getString(cur, ContactsContract.CommonDataKinds.Phone.STARRED) == "1"

                val whereName = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?"
                val whereNameParams = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id)

                val nameCur = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    whereName,
                    whereNameParams,
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME
                )!!
                nameCur.moveToFirst()
                val firstName = getString(nameCur, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                val surname = getString(nameCur, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)

                contacts.add(PhoneContact(id, firstName, surname, avatar, isFavorite))

                nameCur.close()
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
        val avatar: String,
        val isFavorite: Boolean,
    ) {
        fun toPhoneContactDto(): PhoneContactDto = PhoneContactDto(id, avatar, firstName, surname, isFavorite)
    }
}