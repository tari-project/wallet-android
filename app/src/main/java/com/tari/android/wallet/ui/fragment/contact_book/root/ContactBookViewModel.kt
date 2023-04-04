package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import javax.inject.Inject

class ContactBookViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val sharedState = MutableLiveData(false)

    val shareList = MutableLiveData<List<ShareOptionArgs>>()

    init {
        component.inject(this)

        shareList.postValue(
            listOf(
                ShareOptionArgs(
                    resourceManager.getString(R.string.share_contact_via_qr_code),
                    R.drawable.vector_share_qr_code,
                    true
                ) { shareViaQrCode() },
                ShareOptionArgs(resourceManager.getString(R.string.share_contact_via_qr_link), R.drawable.vector_share_link) { shareViaLink() },
                ShareOptionArgs(resourceManager.getString(R.string.share_contact_via_qr_nfc), R.drawable.vector_share_nfc) { shareViaNFC() },
                ShareOptionArgs(resourceManager.getString(R.string.share_contact_via_qr_ble), R.drawable.vector_share_ble) { shareViaBLE() },
            )
        )
    }

    fun setSharedState(state: Boolean) {
        sharedState.value = state
    }

    fun shareSelectedContacts() {
        //todo
        setSharedState(false)
    }

    fun shareViaQrCode() {
        //todo
        setSelectedToPosition(0)
    }

    fun shareViaLink() {
        //todo
        setSelectedToPosition(1)
    }

    fun shareViaNFC() {
        //todo
        setSelectedToPosition(2)
    }

    fun shareViaBLE() {
        //todo
        setSelectedToPosition(3)
    }

    private fun setSelectedToPosition(position: Int) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values[position].isSelected = true
        shareList.postValue(values)
    }
}