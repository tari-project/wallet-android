package com.tari.android.wallet.ui.screen.send.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.extension.composeContent

class SendFragment : CommonFragment<SendViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            SendScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onRecipientAddressChange = { viewModel.onAddressChange(it) },
                onAmountChange = { viewModel.onAmountChange(it) },
                onFeeHelpClicked = { viewModel.onFeeHelpClicked() },
                onContinueClick = { viewModel.onContinueClick() },
                onScanQrClick = { startQrScanner(QrScannerSource.TransactionSend) },
                onContactBookClick = { viewModel.onContactBookClick() },
                onNoteChange = { viewModel.onNoteChange(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: SendViewModel by viewModels()
        bindViewModel(viewModel)
    }


    companion object {
        const val PARAMETER_CONTACT = "PARAMETER_CONTACT"
        const val PARAMETER_AMOUNT = "PARAMETER_AMOUNT"
        const val PARAMETER_NOTE = "PARAMETER_NOTE"

        fun newInstance(
            contact: Contact? = null,
            amount: MicroTari? = null,
            note: String? = null,
        ) = SendFragment().apply {
            arguments = Bundle().apply {
                putParcelable(PARAMETER_CONTACT, contact)
                putParcelable(PARAMETER_AMOUNT, amount)
                putString(PARAMETER_NOTE, note)
            }
        }
    }
}