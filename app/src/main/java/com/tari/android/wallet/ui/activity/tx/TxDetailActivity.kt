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
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.databinding.ActivityTxDetailBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.component.*
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
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
internal class TxDetailActivity : AppCompatActivity(), ServiceConnection {

    companion object {
        const val TX_DETAIL_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"

        fun createIntent(context: Context, transaction: Tx): Intent {
            return Intent(context, TxDetailActivity::class.java)
                .apply {
                    putExtra(TX_DETAIL_EXTRA_KEY, transaction)
                }
        }
    }

    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator)
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

    @BindColor(R.color.black)
    @JvmField
    var blackColor = 0

    @BindColor(R.color.light_gray)
    @JvmField
    var lightGrayColor = 0

    @BindDimen(R.dimen.add_amount_element_text_size)
    @JvmField
    var elementTextSize = 0f

    @BindDimen(R.dimen.add_amount_gem_size)
    @JvmField
    var amountGemSize = 0f

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

    private lateinit var ui: ActivityTxDetailBinding
    private val dimmerViews get() = arrayOf(ui.topDimmerView, ui.bottomDimmerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityTxDetailBinding.inflate(layoutInflater).apply { setContentView(root) }
        TxDetailActivityVisitor.visit(this)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        tx = intent.getParcelableExtra(TX_DETAIL_EXTRA_KEY) as Tx
        setupUi()
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
    private fun setupUi() {
        currentTextSize = elementTextSize
        currentAmountGemSize = amountGemSize

        ui.paymentStateTextView.text = when (tx) {
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

        ui.fromTextView.text = when (tx) {
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
        ui.dateTextView.text = Date(timestamp).txFormattedDate()
        ui.amountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)

        emojiIdSummaryController.display(tx.user.publicKey.emojiId)

        ui.txIdTextView.text = "${getString(R.string.tx_detail_transaction_id)}:${tx.id}"
        if (tx.message.isBlank()) {
            ui.noteLabelTextView.invisible()
        }
        ui.txNoteTextView.text = tx.message
        ui.backView.setOnClickListener { onBackPressed() }
        val user = tx.user
        if (user is Contact) {
            ui.contactContainerView.visible()
            setUIAlias(user.alias)
        } else {
            ui.addContactButton.visible()
            ui.contactContainerView.gone()
        }
        when {
            (tx as? CompletedTx)?.direction == Tx.Direction.OUTBOUND -> {
                ui.txFeeGroup.visible()
                val fee = (tx as CompletedTx).fee
                ui.txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            }
            tx is PendingOutboundTx -> {
                ui.txFeeGroup.visible()
                val fee = (tx as PendingOutboundTx).fee
                ui.txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            }
            else -> {
                ui.txFeeGroup.gone()
            }
        }
        ui.amountContainerView.post { scaleDownAmountTextViewIfRequired() }

        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)
        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            tx.user.publicKey.emojiId,
            emojiIdChunkSeparator,
            blackColor,
            lightGrayColor
        )
        ui.fullEmojiIdContainerView.gone()
        dimmerViews.forEach { dimmerView -> dimmerView.gone() }
        ui.copyEmojiIdContainerView.gone()
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)

        ui.emojiIdSummaryContainerView.setOnClickListener { onEmojiSummaryClicked(it) }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked(it) }
        dimmerViews.forEach { it.setOnClickListener { hideFullEmojiId() } }
        ui.feeLabelTextView.setOnClickListener { showTxFeeToolTip() }
        ui.addContactButton.setOnClickListener { onAddContactClick() }
        ui.editContactLabelTextView.setOnClickListener { onEditContactClick() }
        ui.createContactEditText.setOnEditorActionListener { _, actionId, _ ->
            onContactEditTextEditAction(actionId)
        }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert =
            ui.amountContainerView.getLastChild()!!.right - ui.amountContainerView.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + ui.amountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > ui.amountContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor

        // adjust gem size
        UiUtil.setWidthAndHeight(
            ui.amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        ui.amountTextView.setTextSizePx(currentTextSize)
    }

    private fun onEmojiSummaryClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        // make dimmers non-clickable until the anim is over
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        // prepare views
        ui.emojiIdSummaryContainerView.invisible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = ui.emojiIdContainerView.width - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            ui.fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        ui.fullEmojiIdContainerView.alpha = 0f
        ui.fullEmojiIdContainerView.visible()
        // scroll to end
        ui.fullEmojiIdScrollView.post {
            ui.fullEmojiIdScrollView.scrollTo(
                ui.fullEmojiIdTextView.width - ui.fullEmojiIdScrollView.width,
                0
            )
        }
        ui.copyEmojiIdContainerView.alpha = 0f
        ui.copyEmojiIdContainerView.visible()
        UiUtil.setBottomMargin(
            ui.copyEmojiIdContainerView,
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
            ui.fullEmojiIdContainerView.alpha = value
            ui.fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            ui.fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdContainerView,
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
        ui.fullEmojiIdScrollView.postDelayed({
            ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true) {
        dimmerViews.forEach { UiUtil.temporarilyDisableClick(it) }
        ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        ui.emojiIdSummaryContainerView.visible()
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdContainerView,
                (copyEmojiIdButtonVisibleBottomMargin * value).toInt()
            )
        }
        // emoji id anim
        val fullEmojiIdInitialWidth = ui.emojiIdContainerView.width
        val fullEmojiIdDeltaWidth =
            ui.emojiIdSummaryContainerView.width - ui.emojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // hide overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = (1 - value) * 0.6f
            }
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
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
                    dimmerView.gone()
                }
                ui.fullEmojiIdContainerView.gone()
                ui.copyEmojiIdContainerView.gone()
            }
        })
    }

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            tx.user.publicKey.emojiId
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false)
        }
        val copyEmojiIdButtonAnim = ui.copyEmojiIdContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    private fun onContactEditTextEditAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val alias = ui.createContactEditText.text?.toString()
            if (!alias.isNullOrEmpty()) {
                updateContactAlias(alias)
                setUIAlias(alias)
            }
            ui.contactLabelTextView.setTextColor(contactLabelTxtGrayColor)
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
            (tx.user as? Contact)?.alias = newAlias
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
        ui.contactNameTextView.visible()
        ui.createContactEditText.invisible()
        ui.contactNameTextView.text = alias
        ui.editContactLabelTextView.visible()
        ui.addContactButton.invisible()
    }

    /**
     * Add contact alias.
     */
    private fun onAddContactClick() {
        ui.contactContainerView.visible()
        ui.addContactButton.invisible()
        ui.createContactEditText.visible()
        ui.contactLabelTextView.visible()
        ui.editContactLabelTextView.invisible()

        focusContactEditText()
    }

    private fun focusContactEditText() {
        ui.createContactEditText.post {
            ui.createContactEditText.requestFocus()
            ui.createContactEditText.setSelection(ui.createContactEditText.text?.length ?: 0)
            UiUtil.showKeyboard(this)
        }
        ui.contactLabelTextView.setTextColor(contactLabelTxtBlackColor)
    }

    /**
     * Edit contact alias.
     */
    private fun onEditContactClick() {
        ui.editContactLabelTextView.invisible()
        ui.createContactEditText.visible()
        focusContactEditText()
        val user = tx.user
        if (user is Contact) {
            ui.createContactEditText.setText(user.alias)
        }
        ui.contactNameTextView.invisible()
    }

    private fun showTxFeeToolTip() {
        BottomSlideDialog(
            context = this,
            layoutId = R.layout.tx_fee_tooltip_dialog,
            dismissViewId = R.id.tx_fee_tooltip_dialog_txt_close
        ).show()
    }

    private object TxDetailActivityVisitor {
        internal fun visit(activity: TxDetailActivity) {
            (activity.application as TariWalletApplication).appComponent.inject(activity)
            ButterKnife.bind(activity)
        }
    }

}
