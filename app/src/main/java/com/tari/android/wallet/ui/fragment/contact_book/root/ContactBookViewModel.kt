package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareType
import com.tari.android.wallet.ui.fragment.send.shareQr.ShareQrCodeModule
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
                    ShareType.QR_CODE,
                    resourceManager.getString(R.string.share_contact_via_qr_code),
                    R.drawable.vector_share_qr_code,
                    true
                ) { shareViaQrCode() },
                ShareOptionArgs(
                    ShareType.LINK,
                    resourceManager.getString(R.string.share_contact_via_qr_link),
                    R.drawable.vector_share_link
                ) { shareViaLink() },
                ShareOptionArgs(
                    ShareType.NFC,
                    resourceManager.getString(R.string.share_contact_via_qr_nfc),
                    R.drawable.vector_share_nfc
                ) { shareViaNFC() },
                ShareOptionArgs(
                    ShareType.BLE,
                    resourceManager.getString(R.string.share_contact_via_qr_ble),
                    R.drawable.vector_share_ble
                ) { shareViaBLE() },
            )
        )
    }

    fun setSharedState(state: Boolean) {
        sharedState.value = state
    }

    fun shareViaQrCode() = setSelectedToPosition(0)

    fun shareViaLink() = setSelectedToPosition(1)

    fun shareViaNFC() = setSelectedToPosition(2)

    fun shareViaBLE() = setSelectedToPosition(3)

    fun shareSelectedContacts() {
        //todo
        setSharedState(false)
        val args = shareList.value!!.first { it.isSelected }
        val selectedContacts = contactsRepository.getSelectedContacts()
        when (args.type) {
            ShareType.QR_CODE -> doShareViaQrCode(selectedContacts)
            ShareType.LINK -> doShareViaLink(selectedContacts)
            ShareType.NFC -> doShareViaNFC(selectedContacts)
            ShareType.BLE -> doShareViaBLE(selectedContacts)
        }
    }

    private fun doShareViaQrCode(selectedContacts: List<ContactDto>) {
        //todo
        val deeplink = selectedContacts.toString()
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = true), listOf(
                HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                ShareQrCodeModule(deeplink),
                ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    private fun doShareViaLink(selectedContacts: List<ContactDto>) {
        //todo
    }

    private fun doShareViaNFC(selectedContacts: List<ContactDto>) {
        //todo
    }

    private fun doShareViaBLE(selectedContacts: List<ContactDto>) {
        //todo
    }

    private fun setSelectedToPosition(position: Int) {
        val values = shareList.value!!
        values.forEach { it.isSelected = false }
        values[position].isSelected = true
        shareList.postValue(values)
    }
}