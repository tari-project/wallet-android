package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource

class SelectUserContactFragment : ContactSelectionFragment() {

    companion object {
        fun newInstance(withToolbar: Boolean = true) = SelectUserContactFragment().apply {
            arguments = Bundle().apply { putBoolean("withToolbar", withToolbar) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val withToolbar = arguments?.getBoolean("withToolbar") ?: true
        ui.toolbar.setVisible(withToolbar)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.transaction_send_to)
        ui.addFirstNameInput.gone()

        viewModel.isContactlessPayment.postValue(true)
        viewModel.additionalFilter = { it.contact.getFFIDto() != null }
    }

    override fun startQRCodeActivity() {
        QRScannerActivity.startScanner(this, QrScannerSource.TransactionSend)
    }

    override fun goToNext() {
        super.goToNext()

        val user = viewModel.getUserDto()
        viewModel.navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(user, viewModel.amount.value))
    }
}