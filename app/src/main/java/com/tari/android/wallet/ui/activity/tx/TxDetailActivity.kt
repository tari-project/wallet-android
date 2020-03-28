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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.component.*
import com.tari.android.wallet.ui.extension.getFirstChild
import com.tari.android.wallet.ui.extension.getLastChild
import com.tari.android.wallet.ui.extension.setTextSizePx
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
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
    lateinit var amountContainer: RelativeLayout
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
    @BindView(R.id.tx_detail_vw_emoji_id_summary_container)
    lateinit var emojiIdSummaryContainerView: View
    @BindView(R.id.tx_detail_vw_emoji_id_summary)
    lateinit var emojiIdSummaryView: View
    @BindView(R.id.tx_detail_txt_note_label)
    lateinit var noteLabelView: View
    @BindView(R.id.tx_detail_vw_tx_fee_group)
    lateinit var txFeeGroup: Group
    @BindView(R.id.tx_detail_txt_from)
    lateinit var fromTextView: CustomFontTextView
    /**
     * Dimmers.
     */
    @BindViews(
        R.id.tx_detail_vw_top_dimmer,
        R.id.tx_detail_vw_bottom_dimmer
    )
    lateinit var dimmerViews: List<@JvmSuppressWildcards View>
    @BindView(R.id.tx_detail_vw_emoji_id_container)
    lateinit var emojiIdContainerView: View
    @BindView(R.id.tx_detail_vw_full_emoji_id_container)
    lateinit var fullEmojiIdContainerView: View
    @BindView(R.id.tx_detail_scroll_full_emoji_id)
    lateinit var fullEmojiIdScrollView: HorizontalScrollView
    @BindView(R.id.tx_detail_txt_full_emoji_id)
    lateinit var fullEmojiIdTextView: TextView
    @BindView(R.id.tx_detail_vw_copy_emoji_id_container)
    lateinit var copyEmojiIdButtonContainerView: View
    @BindView(R.id.tx_detail_vw_emoji_id_copied)
    lateinit var emojiIdCopiedAnimView: View

    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String
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
    @BindString(R.string.common_from)
    var paymentFrom = ""
    @JvmField
    @BindString(R.string.common_to)
    var paymentTo = ""

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
    @BindDimen(R.dimen.common_copy_emoji_id_button_visible_bottom_margin)
    @JvmField
    var copyEmojiIdButtonVisibleBottomMargin = 0

    @Inject
    lateinit var tracker: Tracker
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private var walletService: TariWalletService? = null

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f

    private lateinit var tx: Tx
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    /**
     * Animates the emoji id "copied" text.
     */
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    override val contentViewId = R.layout.activity_tx_detail

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
        tx = intent.getParcelableExtra(TX_DETAIL_EXTRA_KEY) as Tx
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

        txPaymentStateTextView.text = when (tx) {
            is CompletedTx -> {
                when (tx.direction) {
                    Tx.Direction.INBOUND -> paymentReceived
                    Tx.Direction.OUTBOUND -> paymentSent
                }
            }
            is PendingInboundTx -> pendingPaymentReceived
            is PendingOutboundTx -> pendingPaymentSent
            else -> throw RuntimeException("Unexpected transaction type for transaction: " + tx.id)
        }

        fromTextView.text = when (tx) {
            is CompletedTx -> {
                when (tx.direction) {
                    Tx.Direction.INBOUND -> paymentFrom
                    Tx.Direction.OUTBOUND -> paymentTo
                }
            }
            is PendingInboundTx -> paymentFrom
            is PendingOutboundTx -> paymentTo
            else -> throw RuntimeException("Unexpected transaction type for transaction: " + tx.id)
        }

        val timestamp = tx.timestamp.toLong() * 1000
        txDateTextView.text = Date(timestamp).txFormattedDate()
        txAmountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)

        emojiIdSummaryController.display(tx.user.publicKey.emojiId)

        txIdTextView.text = "${getString(R.string.tx_detail_transaction_id)}:${tx.id}"
        if (tx.message.isBlank()) {
            noteLabelView.visibility = View.INVISIBLE
        }
        txNoteTv.text = tx.message
        backArrowImageView.setOnClickListener { onBackPressed() }
        val user = tx.user
        if (user is Contact) {
            contactContainerView.visibility = View.VISIBLE
            setUIAlias(user.alias)
        } else {
            addContactButton.visibility = View.VISIBLE
            contactContainerView.visibility = View.GONE
        }
        when {
            (tx as? CompletedTx)?.direction == Tx.Direction.OUTBOUND -> {
                txFeeGroup.visibility = View.VISIBLE
                val fee = (tx as CompletedTx).fee
                txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            }
            tx is PendingOutboundTx -> {
                txFeeGroup.visibility = View.VISIBLE
                val fee = (tx as PendingOutboundTx).fee
                txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            }
            else -> {
                txFeeGroup.visibility = View.GONE
            }
        }
        amountContainer.post { scaleDownAmountTextViewIfRequired() }

        OverScrollDecoratorHelper.setUpOverScroll(fullEmojiIdScrollView)
        fullEmojiIdTextView.text = EmojiUtil.getChunkedEmojiId(
            tx.user.publicKey.emojiId,
            emojiIdChunkSeparator
        )
        fullEmojiIdContainerView.visibility = View.GONE
        dimmerViews.forEach { dimmerView -> dimmerView.visibility = View.GONE }
        copyEmojiIdButtonContainerView.visibility = View.GONE
        emojiIdCopiedViewController = EmojiIdCopiedViewController(emojiIdCopiedAnimView)
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

        // adjust gem size
        UiUtil.setWidthAndHeight(
            amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        txAmountTextView.setTextSizePx(currentTextSize)
    }

    @OnClick(R.id.tx_detail_vw_emoji_id_summary_container)
    fun onEmojiSummaryClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        // make dimmers non-clickable until the anim is over
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        // prepare views
        emojiIdSummaryContainerView.visibility = View.INVISIBLE
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visibility = View.VISIBLE
        }
        val fullEmojiIdInitialWidth = emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = emojiIdContainerView.width - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        fullEmojiIdContainerView.alpha = 0f
        fullEmojiIdContainerView.visibility = View.VISIBLE
        // scroll to end
        fullEmojiIdScrollView.post {
            fullEmojiIdScrollView.scrollTo(
                fullEmojiIdTextView.width - fullEmojiIdScrollView.width,
                0
            )
        }
        copyEmojiIdButtonContainerView.alpha = 0f
        copyEmojiIdButtonContainerView.visibility = View.VISIBLE
        UiUtil.setBottomMargin(
            copyEmojiIdButtonContainerView,
            0
        )
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // display overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = value * 0.6f
            }
            // container alpha & scale
            fullEmojiIdContainerView.alpha = value
            fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                copyEmojiIdButtonContainerView,
                (copyEmojiIdButtonVisibleBottomMargin * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim, copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView -> dimmerView.isClickable = true }
            }
        })
        // scroll animation
        fullEmojiIdScrollView.postDelayed({
            fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true) {
        fullEmojiIdScrollView.smoothScrollTo(0, 0)
        emojiIdSummaryContainerView.visibility = View.VISIBLE
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                copyEmojiIdButtonContainerView,
                (copyEmojiIdButtonVisibleBottomMargin * value).toInt()
            )
        }
        // emoji id anim
        val fullEmojiIdInitialWidth = emojiIdContainerView.width
        val fullEmojiIdDeltaWidth = emojiIdSummaryContainerView.width - emojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // hide overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = (1 - value) * 0.6f
            }
            // container alpha & scale
            fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView ->
                    dimmerView.visibility = View.GONE
                }
                fullEmojiIdContainerView.visibility = View.GONE
                copyEmojiIdButtonContainerView.visibility = View.GONE
            }
        })
    }

    @OnClick(R.id.tx_detail_btn_copy_emoji_id)
    fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val deepLinkClipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            EmojiUtil.getChunkedEmojiId(tx.user.publicKey.emojiId, emojiIdChunkSeparator)
        )
        clipBoard?.setPrimaryClip(deepLinkClipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false)
        }
        val copyEmojiIdButtonAnim = copyEmojiIdButtonContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    /**
     * Dimmer clicked - hide dimmers.
     */
    @OnClick(
        R.id.tx_detail_vw_top_dimmer,
        R.id.tx_detail_vw_bottom_dimmer
    )
    fun onDimmerViewsClicked() {
        hideFullEmojiId()
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
            tx.user.publicKey,
            newAlias,
            error
        )
        if (error.code == WalletErrorCode.NO_ERROR) {
            EventBus.post(
                Event.Contact.ContactAddedOrUpdated(
                    tx.user.publicKey,
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

    @OnClick(R.id.tx_detail_txt_fee_label)
    fun onFeeViewClick() {
        showTxFeeToolTip()
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
        val user = tx.user
        if (user is Contact) {
            contactEditText.setText(user.alias)
        }
        contactNameTextView.visibility = View.INVISIBLE
    }

    private fun showTxFeeToolTip() {
        Dialog(this, R.style.Theme_AppCompat_Dialog).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.tx_fee_tooltip_dialog)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            findViewById<TextView>(R.id.tx_fee_tooltip_dialog_txt_close)
                .setOnClickListener {
                    dismiss()
                }

            window?.setGravity(Gravity.BOTTOM)
        }.show()
    }

}