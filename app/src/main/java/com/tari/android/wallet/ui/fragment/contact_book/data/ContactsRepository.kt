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
        doOnConnectedToWallet {
            val list = (1..10).map { ContactDto.generateContactDto() }.toMutableList()
            publishSubject.onNext(list)
        }
    }

    fun toggleFavorite(contactDto: ContactDto) {
        updateContact(contactDto.uuid) {
            it.isFavorite = !it.isFavorite
        }
    }

    fun deleteContact(contactDto: ContactDto) {
        updateContact(contactDto.uuid) {
            it.isDeleted = true
        }
    }

    fun updateContactName(contact: ContactDto, newName: String) {
        updateContact(contact.uuid) {
            when (val user = it.contact) {
                is FFIContactDto -> user.localAlias = newName
                is PhoneContactDto -> user.name = newName
                is MergedContactDto -> user.phoneContactDto.name = newName
            }
        }
    }

    private fun updateContact(contactUuid: String, updateAction: (contact: ContactDto) -> Unit) {
        val value = publishSubject.value!!
        val foundContact = value.firstOrNull { it.uuid == contactUuid }
        foundContact?.let { contact -> updateAction.invoke(contact) }
        publishSubject.onNext(value)
    }
}