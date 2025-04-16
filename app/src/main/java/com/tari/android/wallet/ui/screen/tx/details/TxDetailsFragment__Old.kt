/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.screen.tx.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.databinding.FragmentTxDetailsBinding
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TxStatus.BROADCAST
import com.tari.android.wallet.model.TxStatus.COINBASE
import com.tari.android.wallet.model.TxStatus.COINBASE_CONFIRMED
import com.tari.android.wallet.model.TxStatus.COINBASE_NOT_IN_BLOCKCHAIN
import com.tari.android.wallet.model.TxStatus.COINBASE_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.COMPLETED
import com.tari.android.wallet.model.TxStatus.IMPORTED
import com.tari.android.wallet.model.TxStatus.MINED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.MINED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.PENDING
import com.tari.android.wallet.model.TxStatus.QUEUED
import com.tari.android.wallet.model.TxStatus.REJECTED
import com.tari.android.wallet.model.TxStatus.TX_NULL_ERROR
import com.tari.android.wallet.model.TxStatus.UNKNOWN
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.model.tx.Tx.Direction.INBOUND
import com.tari.android.wallet.model.tx.Tx.Direction.OUTBOUND
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel.TX_EXTRA_KEY
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.hideKeyboard
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.txFormattedDate
import com.tari.android.wallet.util.extension.visible
import java.util.Date

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
class TxDetailsFragment__Old : CommonXmlFragment<FragmentTxDetailsBinding, TxDetailsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentTxDetailsBinding.inflate(layoutInflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().hideKeyboard()

        val viewModel: TxDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeVM()
    }

    private fun observeVM() = with(viewModel) {
        collectFlow(uiState) { uiState ->
            bindTxData(uiState.tx)
            uiState.contact?.let { updateContactInfo(it) }
            uiState.blockExplorerLink?.let { showExplorerLink(it) }
            ui.paymentStateTextView.text = string(uiState.screenTitle)
        }
    }

    private fun updateContactInfo(contact: ContactDto) {
        val alias = contact.contactInfo.getAlias()
        val addEditText = if (alias.isEmpty()) R.string.tx_detail_add_contact else R.string.tx_detail_edit
        ui.editContactLabelTextView.text = getString(addEditText)
        ui.contactNameTextView.setText(contact.contactInfo.getAlias())
    }

    private fun setupUI() {
        ui.emojiIdSummaryContainerView.setOnClickListener { viewModel.onAddressDetailsClicked() }
        ui.feeLabelTextView.setOnClickListener { viewModel.showTxFeeToolTip() }
        ui.editContactLabelTextView.setOnClickListener { viewModel.onContactEditClicked() }
        ui.cancelTxView.setOnClickListener { viewModel.onTransactionCancel() }
    }

    private fun bindTxData(tx: Tx) {
        ui.userContainer.setVisible(!tx.isOneSided && !tx.isCoinbase)
        ui.contactContainerView.setVisible(!tx.isOneSided && !tx.isCoinbase)

        setTxStatusData(tx)
        setTxMetaData(tx)
        setTxAddressData(tx)
        setTxPaymentData(tx)
    }

    private fun setTxPaymentData(tx: Tx) {
        ui.amountTextView.text = WalletConfig.amountFormatter.format(tx.amount.tariValue)
        setFeeData(viewModel.uiState.value.txFee)
    }

    private fun setFeeData(fee: MicroTari?) {
        if (fee != null) {
            ui.txFeeTextView.visible()
            ui.feeLabelTextView.visible()
            ui.txFeeTextView.text = string(R.string.tx_details_fee_value, WalletConfig.amountFormatter.format(fee.tariValue))
        } else {
            ui.txFeeTextView.gone()
            ui.feeLabelTextView.gone()
        }
    }

    private fun setTxMetaData(tx: Tx) {
        ui.dateTextView.text = Date(tx.timestamp.toLong() * 1000).txFormattedDate()
        if (tx.note.isEmpty()) {
            ui.txNoteTextView.gone()
        } else {
            ui.txNoteTextView.visible()
            ui.txNoteTextView.text = tx.note
        }
    }

    private fun setTxAddressData(tx: Tx) {
        ui.fromTextView.text = if (tx.isInbound) string(R.string.common_from) else string(R.string.common_to)
        if (tx.tariContact.walletAddress.isUnknownUser()) {
            ui.emojiIdViewContainer.root.gone()
            ui.unknownSource.visible()
        } else {
            ui.unknownSource.gone()
            ui.emojiIdViewContainer.root.visible()
            ui.emojiIdViewContainer.textViewEmojiPrefix.text = tx.tariContact.walletAddress.addressPrefixEmojis()
            ui.emojiIdViewContainer.textViewEmojiFirstPart.text = tx.tariContact.walletAddress.addressFirstEmojis()
            ui.emojiIdViewContainer.textViewEmojiLastPart.text = tx.tariContact.walletAddress.addressLastEmojis()
        }
    }

    private fun setTxStatusData(tx: Tx) {
        val statusText = tx.statusString(context = requireContext(), viewModel.uiState.value.requiredConfirmationCount)
        ui.statusTextView.text = statusText
        ui.statusContainerView.visibility = if (statusText.isEmpty()) View.GONE else View.VISIBLE
        if (tx !is CancelledTx && tx.isOutbound && tx.status == PENDING) {
            ui.cancelTxView.setOnClickListener { viewModel.onTransactionCancel() }
            ui.cancelTxView.visible()
        } else if (ui.cancelTxView.isVisible) {
            ui.cancelTxView.setOnClickListener(null)
            ui.cancelTxView.gone()
        }
    }

    private fun showExplorerLink(explorerLink: String) {
        ui.explorerContainerView.setVisible(explorerLink.isNotEmpty())
        ui.explorerContainerView.setOnClickListener { viewModel.openInBlockExplorer() }
    }


    private fun Tx.statusString(context: Context, requiredConfirmationCount: Long?): String {
        val confirmationCount = if (this is CompletedTx) this.confirmationCount.toInt() else null

        return if (this is CancelledTx) "" else when (this.status) {
            PENDING -> when (this.direction) {
                INBOUND -> context.string(R.string.tx_detail_waiting_for_sender_to_complete)
                OUTBOUND -> context.string(R.string.tx_detail_waiting_for_recipient)
            }

            BROADCAST, COMPLETED -> if (requiredConfirmationCount != null) {
                context.string(R.string.tx_detail_completing_final_processing_with_step, 1, requiredConfirmationCount + 1)
            } else {
                context.string(R.string.tx_detail_completing_final_processing)
            }

            MINED_UNCONFIRMED -> if (confirmationCount != null && requiredConfirmationCount != null) {
                context.string(R.string.tx_detail_completing_final_processing_with_step, confirmationCount, requiredConfirmationCount + 1)
            } else {
                context.string(R.string.tx_detail_completing_final_processing)
            }

            TX_NULL_ERROR, IMPORTED, COINBASE, MINED_CONFIRMED, REJECTED, ONE_SIDED_UNCONFIRMED, ONE_SIDED_CONFIRMED, QUEUED, COINBASE_UNCONFIRMED,
            COINBASE_CONFIRMED, COINBASE_NOT_IN_BLOCKCHAIN, UNKNOWN -> ""
        }
    }

    companion object {

        fun newInstance(tx: Tx) = TxDetailsFragment__Old().apply {
            arguments = Bundle().apply {
                putParcelable(TX_EXTRA_KEY, tx)
            }
        }
    }
}
