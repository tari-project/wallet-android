package com.tari.android.wallet.ui.activity.transactiondetails

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.Group
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.component.CustomFontEditText
import com.tari.android.wallet.ui.component.CustomFontTextView
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.util.EmojiUtil
import java.util.*


class TransactionDetailActivity : BaseActivity() {

    override val contentViewId: Int = R.layout.activity_transaction_detail

    companion object {
        val TRANSACTION_EXTRA_KEY = "TRANSACTION_EXTRA_KEY"

        fun createIntent(context: Context, transaction: Tx): Intent {
            return Intent(context, TransactionDetailActivity::class.java)
                .apply {
                    putExtra(TRANSACTION_EXTRA_KEY, transaction)
                }
        }
    }

    @BindView(R.id.tx_detail_payment_state)
    lateinit var transactionTypeTv: CustomFontTextView
    @BindView(R.id.tx_detail_back)
    lateinit var backBtn: ImageView
    @BindView(R.id.tx_detail_date)
    lateinit var transactionDateTv: CustomFontTextView
    @BindView(R.id.transaction_detail_amount)
    lateinit var transactionAmountTv: CustomFontTextView
    @BindView(R.id.tx_detail_tx_fee)
    lateinit var transactionFeeTv: CustomFontTextView

    @BindView(R.id.tx_detail_add_contact)
    lateinit var addContactBtn: CustomFontTextView

    @BindView(R.id.transaction_detail_contact_name)
    lateinit var contactNameTv: CustomFontTextView

    @BindView(R.id.transaction_detail_create_contact)
    lateinit var createContactEt: CustomFontEditText
    @BindView(R.id.contact_label)
    lateinit var contactLabel: CustomFontTextView

    @BindView(R.id.tx_detail_edit_name)
    lateinit var editContactBtn: CustomFontTextView

    @BindView(R.id.transaction_detail_note)
    lateinit var transactionNoteTv: CustomFontTextView

    @BindView(R.id.tx_detail_tx_id)
    lateinit var transactionIdTv: CustomFontTextView

    @BindView(R.id.tx_detail_contact_container)
    lateinit var contactContainer: View

    @BindView(R.id.transaction_detail_emoji_summary)
    lateinit var emojiSummaryView: View
    @BindView(R.id.transaction_detail_separator)
    lateinit var contactSeparator: View

    @BindView(R.id.note_label)
    lateinit var noteLabel: View

    @BindView(R.id.tx_detail_tx_fee_group)
    lateinit var transactionFeeGroup: Group

    private var transaction: Tx? = null
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, android.R.anim.fade_out)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiSummaryView)
        transaction = intent.getParcelableExtra(TRANSACTION_EXTRA_KEY)
        if (transaction == null) finish()
        setupUI()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        when (transaction) {
            is CompletedTx -> {
                when (transaction!!.direction) {
                    Tx.Direction.INBOUND -> transactionTypeTv.text =
                        getString(R.string.payment_received)
                    Tx.Direction.OUTBOUND -> transactionTypeTv.text =
                        getString(R.string.payment_sent)
                }
            }
            is PendingInboundTx -> transactionTypeTv.text =
                getString(R.string.pending_payment_received)
            is PendingOutboundTx -> transactionTypeTv.text =
                getString(R.string.pending_payment_sent)
        }
        transactionAmountTv.text = "%1$,.2f".format(transaction!!.amount.tariValue.toDouble())

        val emojiId =
            EmojiUtil.getEmojiIdForPublicKeyHexString(transaction!!.user.publicKeyHexString)
        emojiIdSummaryController.display(emojiId)
        transactionIdTv.text = "${getString(R.string.transaction_id)}:${transaction!!.id}"
        if (transaction!!.message.isBlank()) {
            noteLabel.visibility = View.INVISIBLE
        }
        transactionNoteTv.text = transaction!!.message
        backBtn.setOnClickListener { onBackPressed() }
        val user = transaction!!.user
        if (user is Contact) {
            contactContainer.visibility = View.VISIBLE
            setAlias(user.alias)
        } else {
            addContactBtn.visibility = View.VISIBLE
            contactContainer.visibility = View.GONE
        }
        if (transaction is CompletedTx) {
            transactionFeeGroup.visibility = View.VISIBLE
            transactionFeeTv.text = "${(transaction as CompletedTx).calculateFee()}"
        } else {
            transactionFeeGroup.visibility = View.GONE
        }
        createContactEt.setOnKeyListener{ _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val name = createContactEt.text?.toString()
                if (name != null) {
                    setAlias(name)
                }
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = transaction!!.timestamp.toLong() * 1000
        formatDate(calendar)

    }

    private fun formatDate(cal: Calendar) {
        val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayStr = "$day${getDayNumberSuffix(day)}"
        val year = cal.get(Calendar.YEAR)
        val amPm = cal.getDisplayName(Calendar.AM_PM, Calendar.SHORT, Locale.getDefault())
        val hour = cal.get(Calendar.HOUR)
        val min = cal.get(Calendar.MINUTE)
        var minStr = "$min"
        var hourStr = "$hour"
        if (min < 10) minStr = "0$min"
        if (hour < 10) hourStr = "0$hour"

        transactionDateTv.text = "$dayOfWeek, $month $dayStr $year at $hourStr:$minStr $amPm"
    }

    private fun getDayNumberSuffix(day: Int): String? {
        return if (day in 11..13) {
            "th"
        } else when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun setAlias(alias: String) {
        contactNameTv.visibility = View.VISIBLE
        createContactEt.visibility = View.INVISIBLE
        contactNameTv.text = alias
        editContactBtn.visibility = View.VISIBLE
        addContactBtn.visibility = View.INVISIBLE
    }

    @OnClick(R.id.tx_detail_add_contact)
    fun addContact() {
        contactContainer.visibility = View.VISIBLE
        addContactBtn.visibility = View.INVISIBLE
        createContactEt.visibility = View.VISIBLE
        contactLabel.visibility = View.VISIBLE
    }

    @OnClick(R.id.tx_detail_edit_name)
    fun editContact() {
        editContactBtn.visibility = View.INVISIBLE
        createContactEt.visibility = View.VISIBLE
        createContactEt.setText((transaction!!.user as Contact).alias)
        contactNameTv.visibility = View.INVISIBLE
    }
}
