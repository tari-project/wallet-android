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
package com.tari.android.wallet.ui.activity.tx

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.Group
import butterknife.*
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.component.CustomFontButton
import com.tari.android.wallet.ui.component.CustomFontEditText
import com.tari.android.wallet.ui.component.CustomFontTextView
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.getFirstChild
import com.tari.android.wallet.ui.extension.getLastChild
import com.tari.android.wallet.ui.extension.setTextSizePx
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.extension.txFormattedDate
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.util.*
import javax.inject.Inject

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
internal class TxDetailActivity :
    BaseActivity(),
    ServiceConnection {

    override val contentViewId = R.layout.activity_tx_detail

    companion object {
        const val TX_DETAIL_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"

        fun createIntent(context: Context, transaction: Tx): Intent {
            return Intent(context, TxDetailActivity::class.java)
                .apply {
                    putExtra(TX_DETAIL_EXTRA_KEY, transaction)
                }
        }
    }

    @BindView(R.id.tx_detail_txt_payment_state)
    lateinit var txPaymentStateTextView: CustomFontTextView
    @BindView(R.id.tx_detail_img_back_arrow)
    lateinit var backArrowImageView: ImageView
    @BindView(R.id.tx_detail_txt_date)
    lateinit var txDateTextView: CustomFontTextView
    @BindView(R.id.tx_detail_txt_amount)
    lateinit var txAmountTextView: CustomFontTextView
    @BindView(R.id.tx_detail_vw_amount_container)
    lateinit var amountContainer: LinearLayout
    @BindView(R.id.tx_detail_img_amount_gem)
    lateinit var amountGemImageView: ImageView
    @BindView(R.id.tx_detail_txt_tx_fee)
    lateinit var txFeeTextView: CustomFontTextView
    @BindView(R.id.tx_detail_btn_add_contact)
    lateinit var addContactButton: CustomFontButton
    @BindView(R.id.tx_detail_txt_contact_name)
    lateinit var contactNameTextView: CustomFontTextView
    @BindView(R.id.tx_detail_edit_create_contact)
    lateinit var contactEditText: CustomFontEditText
    @BindView(R.id.tx_detail_txt_contact_label)
    lateinit var contactLabelTextView: CustomFontTextView
    @BindView(R.id.tx_detail_txt_edit_label)
    lateinit var editContactLabelTextView: CustomFontTextView
    @BindView(R.id.tx_detail_txt_tx_note)
    lateinit var txNoteTv: CustomFontTextView
    @BindView(R.id.tx_detail_txt_tx_id)
    lateinit var txIdTextView: CustomFontTextView
    @BindView(R.id.tx_detail_vw_contact_container)
    lateinit var contactContainerView: View
    @BindView(R.id.tx_detail_vw_emoji_summary)
    lateinit var emojiSummaryView: View
    @BindView(R.id.tx_detail_txt_note_label)
    lateinit var noteLabelView: View
    @BindView(R.id.tx_detail_vw_tx_fee_group)
    lateinit var txFeeGroup: Group

    @JvmField
    @BindString(R.string.tx_detail_payment_received)
    var paymentReceived = ""
    @JvmField
    @BindString(R.string.tx_detail_payment_sent)
    var paymentSent = ""
    @JvmField
    @BindString(R.string.tx_detail_pending_payment_received)
    var pendingPaymentReceived = ""
    @JvmField
    @BindString(R.string.tx_detail_pending_payment_sent)
    var pendingPaymentSent = ""

    @JvmField
    @BindColor(R.color.tx_detail_contact_name_label_text)
    var contactLabelTxtGrayColor = 0
    @JvmField
    @BindColor(R.color.black)
    var contactLabelTxtBlackColor = 0

    @BindDimen(R.dimen.add_amount_element_text_size)
    @JvmField
    var elementTextSize = 0f
    @BindDimen(R.dimen.add_amount_gem_size)
    @JvmField
    var amountGemSize = 0f
    @BindDimen(R.dimen.add_amount_leftmost_digit_margin_start)
    @JvmField
    var firstElementMarginStart = 0

    @Inject
    lateinit var tracker: Tracker

    private var walletService: TariWalletService? = null

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f
    private var elementMarginStart = 0

    private var tx: Tx? = null
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiSummaryView)
        tx = intent.getParcelableExtra(TX_DETAIL_EXTRA_KEY)
        if (tx == null) finish()
        setupUI()

        TrackHelper.track()
            .screen("/home/tx_details")
            .title("Transaction Details")
            .with(tracker)
    }

    override fun onStart() {
        super.onStart()
        // start service if not started yet
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        unbindService(this)
        super.onDestroy()
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service.")
        walletService = TariWalletService.Stub.asInterface(service)
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        walletService = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        currentTextSize = elementTextSize
        currentAmountGemSize = amountGemSize
        elementMarginStart = firstElementMarginStart

        txPaymentStateTextView.text = when (tx) {
            is CompletedTx -> {
                when (tx!!.direction) {
                    Tx.Direction.INBOUND -> paymentReceived
                    Tx.Direction.OUTBOUND -> paymentSent
                }
            }
            is PendingInboundTx -> pendingPaymentReceived
            is PendingOutboundTx -> pendingPaymentSent
            else -> ""
        }

        val timestamp = tx!!.timestamp.toLong() * 1000
        txDateTextView.text = Date(timestamp).txFormattedDate()
        txAmountTextView.text = WalletUtil.amountFormatter.format(tx!!.amount.tariValue)

        emojiIdSummaryController.display(tx!!.user.publicKey.emojiId)

        txIdTextView.text = "${getString(R.string.tx_detail_transaction_id)}:${tx!!.id}"
        if (tx!!.message.isBlank()) {
            noteLabelView.visibility = View.INVISIBLE
        }
        txNoteTv.text = tx!!.message
        backArrowImageView.setOnClickListener { onBackPressed() }
        val user = tx!!.user
        if (user is Contact) {
            contactContainerView.visibility = View.VISIBLE
            setUIAlias(user.alias)
        } else {
            addContactButton.visibility = View.VISIBLE
            contactContainerView.visibility = View.GONE
        }
        if ((tx as? CompletedTx)?.direction == Tx.Direction.OUTBOUND) {
            txFeeGroup.visibility = View.VISIBLE
            val fee = MicroTari((tx as CompletedTx).fee)
            txFeeTextView.text = "+${WalletUtil.feeFormatter.format(fee.tariValue)}"
        } else {
            txFeeGroup.visibility = View.GONE
        }
        amountContainer.post { scaleDownAmountTextViewIfRequired() }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert =
            amountContainer.getLastChild()!!.right - amountContainer.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + txAmountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > amountContainer.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor
        elementMarginStart =
            (elementMarginStart * scaleFactor).toInt()

        // adjust gem size
        UiUtil.setWidthAndHeight(
            amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        txAmountTextView.setTextSizePx(currentTextSize)
    }

    @OnEditorAction(R.id.tx_detail_edit_create_contact)
    fun onContactEditTextEditAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val alias = contactEditText.text?.toString()
            if (!alias.isNullOrEmpty()) {
                updateContactAlias(alias)
                setUIAlias(alias)
            }
            contactLabelTextView.setTextColor(contactLabelTxtGrayColor)
            return false
        }
        return true
    }

    private fun updateContactAlias(newAlias: String) {
        if (walletService == null) {
            return
        }
        val error = WalletError()
        walletService?.updateContactAlias(
            tx!!.user.publicKey,
            newAlias,
            error
        )
        if (error.code == WalletErrorCode.NO_ERROR) {
            EventBus.post(
                Event.Contact.ContactAddedOrUpdated(
                    tx!!.user.publicKey,
                    newAlias
                )
            )
        } else {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun setUIAlias(alias: String) {
        contactNameTextView.visibility = View.VISIBLE
        contactEditText.visibility = View.INVISIBLE
        contactNameTextView.text = alias
        editContactLabelTextView.visibility = View.VISIBLE
        addContactButton.visibility = View.INVISIBLE
    }

    /**
     * Add contact alias.
     */
    @OnClick(R.id.tx_detail_btn_add_contact)
    fun onAddContactClick() {
        contactContainerView.visibility = View.VISIBLE
        addContactButton.visibility = View.INVISIBLE
        contactEditText.visibility = View.VISIBLE
        contactLabelTextView.visibility = View.VISIBLE
        editContactLabelTextView.visibility = View.INVISIBLE

        focusContactEditText()
    }

    private fun focusContactEditText() {
        contactEditText.post {
            contactEditText.requestFocus()
            contactEditText.setSelection(contactEditText.text?.length ?: 0)
            UiUtil.showKeyboard(this)
        }
        contactLabelTextView.setTextColor(contactLabelTxtBlackColor)
    }

    /**
     * Edit contact alias.
     */
    @OnClick(R.id.tx_detail_txt_edit_label)
    fun onEditContactClick() {
        editContactLabelTextView.visibility = View.INVISIBLE
        contactEditText.visibility = View.VISIBLE
        focusContactEditText()
        val user = tx!!.user
        if (user is Contact) {
            contactEditText.setText(user.alias)
        }
        contactNameTextView.visibility = View.INVISIBLE
    }

}