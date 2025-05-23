package com.tari.android.wallet.ui.screen.contactBook.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionViewModel.ContinueButtonEffect
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.string

class SelectUserContactFragment : ContactSelectionFragment() {

    companion object {
        fun newInstance(withToolbar: Boolean = true) = SelectUserContactFragment().apply {
            arguments = Bundle().apply { putBoolean("withToolbar", withToolbar) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val withToolbar = arguments?.getBoolean("withToolbar") != false
        ui.toolbar.setVisible(withToolbar)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.transaction_send_to)
        ui.addFirstNameInput.gone()

        viewModel.additionalFilter = { it.contact.getFFIContactInfo() != null }
    }

    override fun startQRCodeActivity() {
        startQrScanner(QrScannerSource.TransactionSend)
    }

    override fun goToNext() {
        super.goToNext()

        viewModel.onContinueButtonClick(ContinueButtonEffect.SelectUserContact)
    }
}