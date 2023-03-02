package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    private val sharedPrefsRepository: SharedPrefsRepository
) : CommonViewModel() {
    var publishSubject = BehaviorSubject.create<MutableList<ContactDto>>()

    init {
//        doOnConnectedToWallet {
//            val list = (1..10).map { ContactDto.generateContactDto() }.toMutableList()
//            publishSubject.onNext(list)
//        }

        val list = (1..10).map { ContactDto.generateContactDto() }.toMutableList()
        publishSubject.onNext(list)
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

    fun unlinkContact(contact: ContactDto) {
        //todo
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
        publishSubject.onNext(value)
    }
}