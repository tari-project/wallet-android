package com.tari.android.wallet.ui.fragment.contact_book.data

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.yat.YatUser
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    private val sharedPrefsRepository: SharedPrefsRepository
) : CommonViewModel() {
    var publishSubject = BehaviorSubject.create<MutableList<ContactDto>>()

    init {
        val items = mutableListOf<ContactDto>()

        doOnConnectedToWallet {
            val address = it.getWalletAddress()

            items.add(ContactDto(Contact(TariWalletAddress(address.toString(), address.getEmojiId()), "S"), true))
            items.add(ContactDto(Contact(TariWalletAddress(address.toString(), address.getEmojiId()), "Sa"), false))
            items.add(ContactDto(Contact(TariWalletAddress(address.toString(), address.getEmojiId()), "Sam"), true))
            items.add(ContactDto(User(TariWalletAddress(address.toString(), address.getEmojiId())), false))
            items.add(ContactDto(User(TariWalletAddress(address.toString(), address.getEmojiId())), true))
            items.add(ContactDto(YatUser(TariWalletAddress(address.toString(), address.getEmojiId())).also {
                it.yat = address.getEmojiId().extractEmojis().take(3).joinToString()
            }, false))
            items.add(ContactDto(YatUser(TariWalletAddress(address.toString(), address.getEmojiId())).also {
                it.yat = address.getEmojiId().extractEmojis().take(5).joinToString()
            }, true))

            publishSubject.onNext(items)
        }
    }

    fun toggleFavorite(contactDto: ContactDto) {
        val value = publishSubject.value!!
        contactDto.isFavorite = !contactDto.isFavorite
        publishSubject.onNext(value)
    }

    fun deleteContact(contactDto: ContactDto) {
        val value = publishSubject.value!!
        val foundContact = value.firstOrNull { it.uuid == contactDto.uuid }
        value.remove(foundContact)
        publishSubject.onNext(value)
    }
}