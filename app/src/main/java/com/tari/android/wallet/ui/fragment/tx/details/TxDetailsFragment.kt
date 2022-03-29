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
package com.tari.android.wallet.ui.fragment.tx.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.add_amount_element_text_size
import com.tari.android.wallet.R.dimen.add_amount_gem_size
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentTxDetailsBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.model.TxStatus.*
import com.tari.android.wallet.ui.animation.collapseAndHideAnimation
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.tooltipDialog.TooltipDialog
import com.tari.android.wallet.ui.dialog.tooltipDialog.TooltipDialogArgs
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.tx.details.gif.GIFView
import com.tari.android.wallet.ui.fragment.tx.details.gif.GIFViewModel
import com.tari.android.wallet.ui.fragment.tx.details.gif.TxState
import com.tari.android.wallet.ui.presentation.TxNote
import com.tari.android.wallet.util.WalletUtil
import java.util.*
import javax.inject.Inject

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
internal class TxDetailsFragment : CommonFragment<FragmentTxDetailsBinding, TxDetailsViewModel>() {

    @Inject
    lateinit var tracker: Tracker

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentTxDetailsBinding.inflate(layoutInflater, container, false).also { ui = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appComponent.inject(this)

        val viewModel: TxDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        val tx = arguments?.getParcelable<Tx>(TX_EXTRA_KEY)
        if (tx != null) {
            viewModel.setTxArg(tx)
        }

        val txId = arguments?.getParcelable<TxId>(TX_ID_EXTRA_KEY)
        if (txId != null) {
            viewModel.loadTxById(txId)
        }

        if (savedInstanceState == null) {
            tracker.screen(path = "/home/tx_details", title = "Transaction Details")
        }

        setupUI()
        observeVM()
    }

    private fun observeVM() = with(viewModel) {
        observe(tx) {
            fetchGIFIfAttached(it)
            bindTxData(it)
            enableCTAs()
        }

        observe(cancellationReason) { setCancellationReason(it) }

        observe(explorerLink) { showExplorerLink(it) }
    }

    private fun setCancellationReason(text: String) {
        ui.cancellationReasonView.text = text
        ui.cancellationReasonView.setVisible(text.isNotBlank())
    }

    private fun fetchGIFIfAttached(tx: Tx) {
        val gifId = TxNote.fromNote(tx.message).gifId ?: return
        val gifViewModel: GIFViewModel by viewModels()
        gifViewModel.onGIFFetchRequested(gifId)
        GIFView(ui.gifContainer, Glide.with(this), gifViewModel, this).displayGIF()
    }

    private fun setupUI() {
        bindViews()
        setUICommands()
        disableCTAs()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindViews() {
        currentTextSize = dimen(add_amount_element_text_size)
        currentAmountGemSize = dimen(add_amount_gem_size)

        fullEmojiIdViewController = FullEmojiIdViewController(ui.emojiIdOuterContainer, ui.emojiIdSummaryView, requireContext())
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        ui.gifContainer.root.invisible()
        ui.gifContainer.loadingGifProgressBar.setColor(color(tx_list_loading_gif_gray))
    }

    private fun disableCTAs() {
        arrayOf<TextView>(ui.addContactButton, ui.cancelTxView, ui.editContactLabelTextView).forEach {
            it.isEnabled = false
            it.setTextColor(color(disabled_cta))
        }
    }

    private fun enableCTAs() {
        arrayOf<TextView>(ui.addContactButton, ui.cancelTxView, ui.editContactLabelTextView).forEach { it.isEnabled = true }
        ui.addContactButton.setTextColor(color(purple))
        ui.editContactLabelTextView.setTextColor(color(purple))
        ui.cancelTxView.setTextColor(color(tx_detail_cancel_tx_cta))
    }

    private fun setUICommands() {
        ui.backView.setOnClickListener { requireActivity().onBackPressed() }
        ui.emojiIdSummaryContainerView.setOnClickListener { onEmojiSummaryClicked(it) }
        ui.feeLabelTextView.setOnClickListener { showTxFeeToolTip() }
        ui.addContactButton.setOnClickListener { onAddContactClick() }
        ui.editContactLabelTextView.setOnClickListener { onEditContactClick() }
        ui.createContactEditText.setOnEditorActionListener { _, actionId, _ -> onContactEditTextEditAction(actionId) }
        ui.cancelTxView.setOnClickListener { onTransactionCancel() }
    }

    private fun bindTxData(tx: Tx) {
        ui.userContainer.setVisible(!tx.isOneSided)
        ui.contactContainerView.setVisible(!tx.isOneSided)

        setTxStatusData(tx)
        setTxMetaData(tx)
        setTxAddresseeData(tx)
        setTxPaymentData(tx)
        setFullEmojiId(tx)
    }

    private fun setTxPaymentData(tx: Tx) {
        val state = TxState.from(tx)
        ui.amountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        ui.paymentStateTextView.text = when {
            tx is CancelledTx -> string(tx_detail_payment_cancelled)
            state.status == MINED_CONFIRMED || state.status == IMPORTED ->
                if (state.direction == INBOUND) string(tx_detail_payment_received)
                else string(tx_detail_payment_sent)
            else -> string(tx_detail_pending_payment_received)
        }
        when {
            tx is CompletedTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is CancelledTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is PendingOutboundTx -> setFeeData(tx.fee)
            else -> {
                ui.txFeeTextView.gone()
                ui.feeLabelTextView.gone()
            }
        }
        scaleDownAmountTextViewIfRequired()
    }

    private fun setFullEmojiId(tx: Tx) {
        fullEmojiIdViewController.fullEmojiId = tx.user.publicKey.emojiId
        fullEmojiIdViewController.emojiIdHex = tx.user.publicKey.hexString
    }

    private fun setFeeData(fee: MicroTari) {
        ui.txFeeTextView.visible()
        ui.feeLabelTextView.visible()
        ui.txFeeTextView.text = string(tx_details_fee_value, WalletUtil.amountFormatter.format(fee.tariValue))
    }

    private fun setTxMetaData(tx: Tx) {
        ui.dateTextView.text = Date(tx.timestamp.toLong() * 1000).txFormattedDate()
        val note = TxNote.fromNote(tx.message)
        if (note.message == null) {
            ui.txNoteTextView.gone()
        } else {
            ui.txNoteTextView.text = if (tx.isOneSided) string(tx_list_you_received_one_side_payment) else note.message
        }
        ui.noteDivider.setVisible(!tx.isOneSided)
        ui.gifContainer.root.visible()
    }

    private fun setTxAddresseeData(tx: Tx) {
        val user = tx.user
        if (user is Contact) {
            ui.contactContainerView.visible()
            setUIAlias(user.alias)
        } else {
            ui.addContactButton.visible()
            ui.contactContainerView.gone()
        }
        val state = TxState.from(tx)
        ui.fromTextView.text =
            if (state.direction == INBOUND) string(common_from) else string(common_to)
        emojiIdSummaryController.display(tx.user.publicKey.emojiId)
    }

    private fun setTxStatusData(tx: Tx) {
        val state = TxState.from(tx)

        val statusText = when {
            tx is CancelledTx -> ""
            state == TxState(INBOUND, PENDING) -> string(tx_detail_waiting_for_sender_to_complete)
            state == TxState(OUTBOUND, PENDING) -> string(tx_detail_waiting_for_recipient)
            state.status != MINED_CONFIRMED -> string(
                tx_detail_completing_final_processing,
                if (tx is CompletedTx) tx.confirmationCount.toInt() + 1 else 1,
                viewModel.requiredConfirmationCount + 1
            )
            else -> ""
        }
        ui.statusTextView.text = statusText
        ui.statusContainerView.visibility = if (statusText.isEmpty()) View.GONE else View.VISIBLE
        if (tx !is CancelledTx && state.direction == OUTBOUND && state.status == PENDING) {
            ui.cancelTxContainerView.setOnClickListener { onTransactionCancel() }
            ui.cancelTxContainerView.visible()
        } else if (ui.cancelTxContainerView.visibility == View.VISIBLE) {
            ui.cancelTxContainerView.setOnClickListener(null)
            collapseAndHideAnimation(ui.cancelTxContainerView).start()
        }
    }

    private fun showExplorerLink(explorerLink: String) {
        ui.explorerContainerView.setVisible(explorerLink.isNotEmpty())
        ui.explorerContainerView.setOnClickListener { viewModel.openInBlockExplorer() }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert = ui.amountContainerView.getLastChild()!!.right - ui.amountContainerView.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + ui.amountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > ui.amountContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor

        // adjust gem size
        ui.amountGemImageView.setLayoutSize(currentAmountGemSize.toInt(), currentAmountGemSize.toInt())
        ui.amountTextView.setTextSizePx(currentTextSize)
    }

    private fun onEmojiSummaryClicked(view: View) {
        view.temporarilyDisableClick()
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun onContactEditTextEditAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val alias = ui.createContactEditText.text?.toString()
            if (!alias.isNullOrEmpty()) {
                viewModel.updateContactAlias(alias)
                setUIAlias(alias)
            } else {
                viewModel.removeContact()
            }
            ui.contactLabelTextView.setTextColor(color(tx_detail_contact_name_label_text))
            return false
        }
        return true
    }

    private fun onTransactionCancel() {
        val tx = viewModel.tx.value!!
        if (tx is PendingOutboundTx && tx.direction == OUTBOUND && tx.status == PENDING) showTxCancelDialog() else Logger.e(
            "cancelTransaction was issued, but current transaction is not pending outbound, but rather $tx"
        )
    }

    private fun showTxCancelDialog() {
        BottomSlideDialog(requireContext(), R.layout.dialog_cancel_tx, dismissViewId = R.id.cancel_tx_dialog_not_cancel_button).apply {
            findViewById<Button>(R.id.cancel_tx_dialog_cancel_button)
                .setOnClickListener {
                    viewModel.cancelTransaction()
                    dismiss()
                }
        }.show()
    }

    private fun setUIAlias(alias: String) {
        ui.contactNameTextView.visible()
        ui.createContactEditText.invisible()
        ui.contactNameTextView.text = alias
        ui.editContactLabelTextView.visible()
        ui.addContactButton.invisible()
    }

    private fun onAddContactClick() {
        ui.contactContainerView.visible()
        ui.addContactButton.invisible()
        ui.createContactEditText.visible()
        ui.contactLabelTextView.visible()
        ui.editContactLabelTextView.invisible()

        focusContactEditText()
    }

    private fun onEditContactClick() {
        ui.editContactLabelTextView.invisible()
        ui.createContactEditText.visible()
        focusContactEditText()
        val user = viewModel.tx.value!!.user
        if (user is Contact) {
            ui.createContactEditText.setText(user.alias)
        }
        ui.contactNameTextView.invisible()
    }

    private fun focusContactEditText() {
        ui.createContactEditText.requestFocus()
        ui.createContactEditText.setSelection(ui.createContactEditText.text?.length ?: 0)
        requireActivity().showKeyboard()
        ui.contactLabelTextView.setTextColor(color(black))
    }

    private fun showTxFeeToolTip() {
        TooltipDialog(requireContext(), TooltipDialogArgs(string(tx_detail_fee_tooltip_transaction_fee), string(tx_detail_fee_tooltip_desc))).show()
    }

    companion object {
        const val TX_EXTRA_KEY = "TX_EXTRA_KEY"
        const val TX_ID_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"
    }
}

