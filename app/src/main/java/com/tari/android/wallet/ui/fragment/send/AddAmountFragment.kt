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
package com.tari.android.wallet.ui.fragment.send

import android.animation.*
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.getFirstChild
import com.tari.android.wallet.ui.extension.getLastChild
import com.tari.android.wallet.ui.extension.setTextSizePx
import com.tari.android.wallet.ui.extension.setWidthAndHeightToMeasured
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.extension.remap
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.math.BigInteger
import javax.inject.Inject
import kotlin.math.min

/**
 * Amount entry fragment.
 *
 * @author The Tari Development Team
 */
class AddAmountFragment(private val walletService: TariWalletService) : BaseFragment() {

    @BindView(R.id.add_amount_vw_root)
    lateinit var rootView: View
    @BindView(R.id.add_amount_txt_title)
    lateinit var titleTextView: TextView
    @BindView(R.id.add_amount_btn_back)
    lateinit var backButton: ImageButton
    @BindView(R.id.add_amount_vw_emoji_id_summary_container)
    lateinit var emojiIdSummaryContainerView: View
    @BindView(R.id.add_amount_vw_emoji_id_summary)
    lateinit var emojiIdSummaryView: View
    @BindView(R.id.add_amount_vw_full_emoji_id_container)
    lateinit var fullEmojiIdContainerView: View
    @BindView(R.id.add_amount_scroll_full_emoji_id)
    lateinit var fullEmojiIdScrollView: HorizontalScrollView
    @BindView(R.id.add_amount_txt_full_emoji_id)
    lateinit var fullEmojiIdTextView: TextView
    @BindView(R.id.add_amount_vw_copy_emoji_id_container)
    lateinit var copyEmojiIdButtonContainerView: View
    @BindView(R.id.add_amount_vw_emoji_id_copied)
    lateinit var emojiIdCopiedAnimView: View
    @BindView(R.id.add_amount_vw_not_enough_balance)
    lateinit var notEnoughBalanceView: View
    @BindView(R.id.add_amount_vw_amount_outer_container)
    lateinit var elementOuterContainerView: ViewGroup
    @BindView(R.id.add_amount_vw_amount_element_container)
    lateinit var elementContainerView: ViewGroup
    @BindView(R.id.add_amount_txt_amount_element_0)
    lateinit var element0TextView: TextView
    @BindView(R.id.add_amount_img_amount_gem)
    lateinit var amountGemImageView: ImageView
    @BindView(R.id.add_amount_vw_amount_center_correction)
    lateinit var amountCenterCorrectionView: View
    @BindView(R.id.add_amount_txt_available_balance)
    lateinit var availableBalanceTextView: TextView
    @BindView(R.id.add_amount_vw_tx_fee_container)
    lateinit var txFeeContainerView: View
    @BindView(R.id.add_amount_txt_tx_fee)
    lateinit var txFeeTextView: TextView
    @BindView(R.id.add_amount_btn_continue_disabled)
    lateinit var disabledContinueButton: Button
    @BindView(R.id.add_amount_btn_continue)
    lateinit var continueButton: Button
    @BindView(R.id.add_amount_btn_decimal_point)
    lateinit var decimalSeparatorButton: Button
    /**
     * Overlay dimmer.
     */
    @BindView(R.id.add_amount_vw_dimmer)
    lateinit var dimmerView: View
    @BindView(R.id.add_amount_vw_full_emoji_id_bg_click_blocker)
    lateinit var fullEmojiIdBgClickBlockerView: View

    /**
     * An element can be a digit or a decimal/thousands separator.
     */
    @BindDimen(R.dimen.add_amount_element_text_size)
    @JvmField
    var elementTextSize = 0f
    @BindDimen(R.dimen.add_amount_element_container_translation_y)
    @JvmField
    var amountElementContainerTranslationY = 0
    @BindDimen(R.dimen.add_amount_gem_size)
    @JvmField
    var amountGemSize = 0f
    @BindDimen(R.dimen.add_amount_leftmost_digit_margin_start)
    @JvmField
    var firstElementMarginStart = 0
    @BindDimen(R.dimen.add_amount_available_balance_error_amount_nudge_distance)
    @JvmField
    var validationErrorNudgeDistance = 0
    @BindDimen(R.dimen.common_horizontal_margin)
    @JvmField
    var horizontalMargin = 0
    @BindDimen(R.dimen.common_copy_emoji_id_button_visible_bottom_margin)
    @JvmField
    var copyEmojiIdButtonVisibleBottomMargin = 0

    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String

    @Inject
    lateinit var tracker: Tracker

    /**
     * Maps all the elements (digits & separators) displayed in the amount text to their
     * corresponding TextViews.
     */
    private val elements = mutableListOf<Pair<String, TextView>>()
    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f
    private var currentFirstElementMarginStart = 0

    /**
     * Whether digit entry animation is running.
     */
    private var digitAnimIsRunning = false
    /**
     * Minimum amount is micro Tari.
     */
    private val maxNoOfDecimalPlaces = 6
    private val thousandsGroupSize = 3
    /**
     * Wait this long before taking action (validation etc.) on the entered amount.
     */
    private val actionWaitLengthMs = 500L

    private val decimalSeparator =
        WalletUtil.amountFormatter.decimalFormatSymbols.decimalSeparator.toString()
    private val thousandsSeparator =
        WalletUtil.amountFormatter.decimalFormatSymbols.groupingSeparator.toString()

    /**
     * Below two are related to amount check and validation.
     */
    private val amountCheckHandler = Handler(Looper.getMainLooper())
    private val amountCheckRunnable = AmountCheckRunnable()

    private val wr = WeakReference(this)
    private lateinit var listenerWR: WeakReference<Listener>

    /**
     * Recipient is either an emoji id or a user from contacts or recent txs.
     */
    private lateinit var recipientUser: User

    /**
     * Formats the summarized emoji id.
     */
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    /**
     * Animates the emoji id "copied" text.
     */
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    override val contentViewId: Int = R.layout.fragment_add_amount

    companion object {

        fun newInstance(walletService: TariWalletService): AddAmountFragment {
            return AddAmountFragment(walletService)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipientUser = arguments!!.getParcelable("recipientUser")!!

        decimalSeparatorButton.text = decimalSeparator

        currentTextSize = elementTextSize
        currentAmountGemSize = amountGemSize
        currentFirstElementMarginStart = firstElementMarginStart

        // hide validation
        notEnoughBalanceView.visibility = View.INVISIBLE
        // hide tx fee
        txFeeContainerView.visibility = View.INVISIBLE
        // hide/disable continue button
        continueButton.visibility = View.INVISIBLE
        disabledContinueButton.visibility = View.VISIBLE
        // add first digit to the element list
        elements.add(Pair("0", element0TextView))

        fullEmojiIdBgClickBlockerView.isClickable = false
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
        displayAliasOrEmojiId()
        emojiIdCopiedViewController = EmojiIdCopiedViewController(emojiIdCopiedAnimView)
        hideFullEmojiId(animated = false)
        OverScrollDecoratorHelper.setUpOverScroll(fullEmojiIdScrollView)

        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    UiUtil.setTopMargin(
                        txFeeContainerView,
                        elementContainerView.bottom
                    )
                    UiUtil.setTopMargin(
                        fullEmojiIdContainerView,
                        emojiIdSummaryContainerView.top
                    )
                    UiUtil.setHeight(
                        fullEmojiIdContainerView,
                        emojiIdSummaryContainerView.height
                    )
                    UiUtil.setWidth(
                        fullEmojiIdContainerView,
                        emojiIdSummaryContainerView.width
                    )
                }
            })

        TrackHelper.track()
            .screen("/home/send_tari/add_amount")
            .title("Send Tari - Add Amount")
            .with(tracker)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    private fun displayAliasOrEmojiId() {
        if (recipientUser is Contact) {
            emojiIdSummaryContainerView.visibility = View.GONE
            titleTextView.visibility = View.VISIBLE
            titleTextView.text = (recipientUser as Contact).alias
        } else {
            displayEmojiId(recipientUser.publicKey.emojiId)
        }
    }

    private fun displayEmojiId(emojiId: String) {
        emojiIdSummaryContainerView.visibility = View.VISIBLE
        emojiIdSummaryController.display(emojiId)
        titleTextView.visibility = View.GONE
        fullEmojiIdTextView.text = EmojiUtil.getChunkedEmojiId(emojiId, emojiIdChunkSeparator)
    }

    override fun onDetach() {
        amountCheckHandler.removeCallbacks(amountCheckRunnable)
        super.onDetach()
    }

    @OnClick(R.id.add_amount_btn_back)
    fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val mActivity = activity ?: return
        mActivity.onBackPressed()
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    @OnClick(R.id.add_amount_vw_emoji_id_summary_container)
    fun emojiIdClicked() {
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        fullEmojiIdBgClickBlockerView.isClickable = true
        // make dimmers non-clickable until the anim is over
        dimmerView.isClickable = false
        // prepare views
        emojiIdSummaryContainerView.visibility = View.INVISIBLE
        dimmerView.alpha = 0f
        dimmerView.visibility = View.VISIBLE
        val fullEmojiIdInitialWidth = emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = (rootView.width - horizontalMargin * 2) - fullEmojiIdInitialWidth
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
            dimmerView.alpha = value * 0.6f
            // container alpha & scale
            fullEmojiIdContainerView.alpha = value
            fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            backButton.alpha = 1 - value
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
                dimmerView.isClickable = true
            }
        })
        // scroll animation
        fullEmojiIdScrollView.postDelayed({
            fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true, animated: Boolean) {
        if (!animated) {
            fullEmojiIdContainerView.visibility = View.GONE
            fullEmojiIdTextView.text = EmojiUtil.getChunkedEmojiId(
                recipientUser.publicKey.emojiId,
                emojiIdChunkSeparator
            )
            dimmerView.visibility = View.GONE
            copyEmojiIdButtonContainerView.visibility = View.GONE
            return
        }
        fullEmojiIdScrollView.smoothScrollTo(0, 0)
        emojiIdSummaryContainerView.visibility = View.VISIBLE
        emojiIdSummaryContainerView.alpha = 0f
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
        val fullEmojiIdInitialWidth = fullEmojiIdContainerView.width
        val fullEmojiIdDeltaWidth = emojiIdSummaryContainerView.width - fullEmojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            dimmerView.alpha = (1 - value) * 0.6f
            // container alpha & scale
            fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            emojiIdSummaryContainerView.alpha = value
            backButton.alpha = value
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
                dimmerView.visibility = View.GONE
                fullEmojiIdBgClickBlockerView.isClickable = false
                fullEmojiIdContainerView.visibility = View.GONE
                copyEmojiIdButtonContainerView.visibility = View.GONE
            }
        })
    }

    /**
     * Dimmer clicked.
     */
    @OnClick(R.id.add_amount_vw_dimmer)
    fun onEmojiIdDimmerClicked() {
        hideFullEmojiId(animated = true)
    }

    @OnClick(R.id.add_amount_btn_copy_emoji_id)
    fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        dimmerView.isClickable = false
        val mActivity = activity ?: return
        val clipBoard = ContextCompat.getSystemService(mActivity, ClipboardManager::class.java)
        val deepLinkClipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            EmojiUtil.getChunkedEmojiId(recipientUser.publicKey.emojiId, emojiIdChunkSeparator)
        )
        clipBoard?.setPrimaryClip(deepLinkClipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false, animated = true)
        }
        // hide copy emoji id button
        val copyEmojiIdButtonAnim = copyEmojiIdButtonContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    @OnClick(R.id.add_amount_txt_tx_fee_desc)
    fun onFeeViewClick() {
        showTxFeeToolTip()
    }

    private fun showTxFeeToolTip() {
        val mActivity = activity ?: return
        Dialog(mActivity, R.style.Theme_AppCompat_Dialog).apply {
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

    /**
     * -1 if no decimal separator, index otherwise.
     */
    private val decimalSeparatorIndex: Int
        get() {
            for ((index, element) in elements.iterator().withIndex()) {
                if (element.first == decimalSeparator) {
                    return index
                }
            }
            return -1
        }


    /**
     * Inserts thousands separators.
     *
     * @return delta width introduced by the removal or addition of thousands separators
     */
    private fun updateThousandsSeparators(): Int {
        var deltaWidth = 0
        // clear existing thousands separators
        val itemsToRemove = mutableListOf<Pair<String, TextView>>()
        for (element in elements.iterator()) {
            if (element.first == thousandsSeparator) {
                elementContainerView.removeView(element.second)
                itemsToRemove.add(element)
                deltaWidth -= element.second.width
            }
        }
        elements.removeAll(itemsToRemove)

        // insert new thousands separators
        val decimalIndex = decimalSeparatorIndex
        val onesEndIndex: Int
        onesEndIndex = if (decimalIndex >= 1) { // no thousands
            if (decimalIndex < (thousandsGroupSize + 1)) {
                return deltaWidth
            } else {
                decimalIndex
            }
        } else {
            elements.size
        }
        for (index in (onesEndIndex - 1) downTo 1) {
            if ((onesEndIndex - index) % thousandsGroupSize == 0) {
                val element = insertThousandsSeparator(index)
                element.second.measure(0, 0)
                deltaWidth += element.second.measuredWidth
            }
        }
        return deltaWidth
    }

    /**
     * Calculates the current amount in microtaris.
     */
    private val currentAmount: MicroTari
        get() {
            var numberOfZerosToAppend = 6 // for millions
            val decimalIndex = decimalSeparatorIndex
            if (decimalIndex > 0) {
                numberOfZerosToAppend -= (elements.size - decimalIndex - 1)
            }
            val amountStringBuilder = StringBuilder()
            for (element in elements) {
                if (element.first != thousandsSeparator && element.first != decimalSeparator) {
                    amountStringBuilder.append(element.first)
                }
            }
            for (i in 0 until numberOfZerosToAppend) {
                amountStringBuilder.append("0")
            }
            return MicroTari(BigInteger(amountStringBuilder.toString()))
        }

    /**
     * Digit or separator clicked.
     */
    @OnClick(
        R.id.add_amount_btn_1,
        R.id.add_amount_btn_2,
        R.id.add_amount_btn_3,
        R.id.add_amount_btn_4,
        R.id.add_amount_btn_5,
        R.id.add_amount_btn_6,
        R.id.add_amount_btn_7,
        R.id.add_amount_btn_8,
        R.id.add_amount_btn_9,
        R.id.add_amount_btn_0,
        R.id.add_amount_btn_decimal_point
    )
    fun onDigitOrSeparatorClicked(view: View) {
        val digit = when (view.id) {
            R.id.add_amount_btn_1 -> "1"
            R.id.add_amount_btn_2 -> "2"
            R.id.add_amount_btn_3 -> "3"
            R.id.add_amount_btn_4 -> "4"
            R.id.add_amount_btn_5 -> "5"
            R.id.add_amount_btn_6 -> "6"
            R.id.add_amount_btn_7 -> "7"
            R.id.add_amount_btn_8 -> "8"
            R.id.add_amount_btn_9 -> "9"
            R.id.add_amount_btn_0 -> "0"
            R.id.add_amount_btn_decimal_point -> decimalSeparator
            else -> throw RuntimeException("Unexpected button clicked with id: " + view.id)
        }
        if (digitAnimIsRunning) return
        // check if entering first digit
        var enteringFirstDigit = false
        if (elements.size == 1 && elements[0].first == "0") {
            // entering first digit as 0 & it's already 0 - return
            if (digit == "0") return
            enteringFirstDigit = (digit != decimalSeparator)
        }
        // check maximum number of decimal places
        for ((index, element) in elements.iterator().withIndex()) {
            if (element.first == decimalSeparator) {
                val noOfDecimalPlaces = elements.size - index - 1
                if (noOfDecimalPlaces == maxNoOfDecimalPlaces) {
                    return
                }
                break
            }
        }
        // exit if entering decimal point when a decimal point exists
        val enteringDecimalPoint = (digit == decimalSeparator)
        if (enteringDecimalPoint && decimalSeparatorIndex > 0) {
            return
        }

        // checks successful - remove any pending validation check
        amountCheckHandler.removeCallbacks(amountCheckRunnable)
        // set the running flag
        digitAnimIsRunning = true
        // current content width
        var contentWidthPreInsert =
            elementContainerView.getLastChild()!!.right - elementContainerView.getFirstChild()!!.left
        // create new text view
        val textView = appendAndGetNewDigitTextView(
            enteringFirstDigit,
            enteringDecimalPoint,
            digit
        )
        // measure it
        textView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val measuredWidth = textView.measuredWidth

        // update thousands separators & get delta width
        contentWidthPreInsert += updateThousandsSeparators()

        // adjust text sizes if content overflows
        scaleDownIfRequired(
            contentWidthPreInsert,
            enteringFirstDigit,
            enteringDecimalPoint,
            textView
        )
        // no animation if entering decimal point
        if (enteringDecimalPoint) {
            digitAnimIsRunning = false
            return
        }
        // otherwise animate the new digit
        animateNewDigit(
            enteringFirstDigit,
            measuredWidth,
            textView,
            digit
        )

        // setup amount validation
        amountCheckHandler.postDelayed(
            amountCheckRunnable,
            actionWaitLengthMs
        )
    }

    /**
     * Inserts the new element after a num-pad click.
     * @return the text view for the new digit
     */
    private fun appendAndGetNewDigitTextView(
        enteringFirstDigit: Boolean,
        enteringDecimalPoint: Boolean,
        digit: String
    ): TextView {
        val inflater = LayoutInflater.from(context)
        return if (enteringFirstDigit) { // first digit :: use the existing view
            elements[0].second
        } else { // inflate text view
            val textView = inflater.inflate(
                R.layout.add_amount_element,
                elementContainerView,
                false
            ) as TextView
            textView.setTextSizePx(currentTextSize)
            if (!enteringDecimalPoint && !enteringFirstDigit) {
                textView.alpha = 0f
                UiUtil.setWidth(textView, 0)
            } else {
                textView.setWidthAndHeightToMeasured()
            }
            textView.text = digit
            // add child
            elementContainerView.addView(textView, elementContainerView.childCount - 1)
            // add new element
            elements.add(Pair(digit, textView))
            textView
        }
    }

    /**
     * Inserts a thousands separator at the specified index.
     *
     * @return inserted element
     */
    private fun insertThousandsSeparator(index: Int): Pair<String, TextView> {
        val inflater = LayoutInflater.from(context)
        val textView = inflater.inflate(
            R.layout.add_amount_element,
            elementContainerView,
            false
        ) as TextView
        textView.setTextSizePx(currentTextSize)
        textView.setWidthAndHeightToMeasured()
        textView.text = thousandsSeparator
        // add child - add 1 for the gem icon (it's the very first child)
        elementContainerView.addView(textView, index + 1)
        val element = Pair(thousandsSeparator, textView)
        elements.add(index, element)
        return element
    }

    /**
     * Scales the elements down if the amount overflows after adding the new digit.
     */
    private fun scaleDownIfRequired(
        contentWidthPreInsert: Int,
        enteringFirstDigit: Boolean,
        enteringDecimalPoint: Boolean,
        textView: TextView
    ) {
        val contentWidthPostInsert = contentWidthPreInsert + textView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > elementContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor
        currentFirstElementMarginStart =
            (currentFirstElementMarginStart * scaleFactor).toInt()

        // adjust gem size
        UiUtil.setWidthAndHeight(
            amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        UiUtil.setStartMargin(
            elements[0].second,
            currentFirstElementMarginStart
        )
        // set center correction view width
        UiUtil.setWidth(
            amountCenterCorrectionView,
            currentAmountGemSize.toInt() + currentFirstElementMarginStart
        )
        // set text sizes
        for (element in elements) {
            element.second.setTextSizePx(currentTextSize)
            if (enteringFirstDigit
                || enteringDecimalPoint
                || element !== elements.last()
            ) {
                element.second.setWidthAndHeightToMeasured()
            } else {
                UiUtil.setWidth(textView, 0)
            }
        }
    }

    /**
     * Animates the introduction of a new digit.
     */
    private fun animateNewDigit(
        enteringFirstDigit: Boolean,
        measuredWidth: Int,
        textView: TextView,
        digit: String
    ) {
        val inflater = LayoutInflater.from(context)
        var ghostTextView = inflater.inflate(
            R.layout.add_amount_element,
            elementOuterContainerView,
            false
        ) as TextView?
        ghostTextView!!.text = digit
        ghostTextView.setWidthAndHeightToMeasured()
        ghostTextView.alpha = 0f
        elementOuterContainerView.addView(ghostTextView)

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            // update text view
            val value = min(1f, valueAnimator.animatedValue as Float)
            if (!enteringFirstDigit) {
                // horizontal scaling if not entering first digit
                val width = measuredWidth * value
                UiUtil.setWidth(textView, width.toInt())
            } else {
                // alpha animation if entering first digit
                textView.alpha = (1 - value) / 2f
            }

            val location = IntArray(2)
            textView.getLocationOnScreen(location)
            // update ghost text view
            ghostTextView!!.setTextSizePx((currentTextSize / 2f) + ((currentTextSize / 2f) * value))
            ghostTextView!!.setWidthAndHeightToMeasured()
            if (enteringFirstDigit) {
                UiUtil.setStartMargin(
                    ghostTextView!!,
                    (location[0] + ((1 - value) * measuredWidth * 0.8f)).toInt()
                )
            } else {
                UiUtil.setStartMargin(
                    ghostTextView!!,
                    (location[0] + ((1 - value) * measuredWidth * 0.3f)).toInt()
                )
            }
            UiUtil.setTopMargin(
                ghostTextView!!,
                elementContainerView.top
                        + amountElementContainerTranslationY
                        + (elementContainerView.height * (1 - value) * 0.8f).toInt()
            )
            ghostTextView!!.alpha = value

            if (value == 1f) {
                if (enteringFirstDigit) {
                    textView.text = digit
                    textView.setWidthAndHeightToMeasured()
                }
                textView.alpha = 1f
                elementOuterContainerView.removeView(ghostTextView)
                ghostTextView = null
                val lastIndex = elements.size - 1
                elements[lastIndex] = elements[lastIndex].copy(first = digit)
                digitAnimIsRunning = false
            }

        }
        anim.duration = Constants.UI.AddAmount.numPadDigitEnterAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.SINE_OUT)
        anim.start()

    }

    @OnClick(R.id.add_amount_btn_delete)
    fun deleteButtonClicked() {
        amountCheckHandler.removeCallbacks(amountCheckRunnable)
        if (elements.size == 1) { // single digit
            elements[0] = elements[0].copy(first = "0")
            elements[0].second.text = elements[0].first
            amountCheckHandler.postDelayed(
                amountCheckRunnable,
                actionWaitLengthMs
            )
            return
        }
        // remove last element
        val removed = elements.removeAt(elements.size - 1)
        elementContainerView.removeView(removed.second)
        // thousands separators
        updateThousandsSeparators()
        // scale
        scaleUpIfRequired()
        // setup amount check/validation
        amountCheckHandler.postDelayed(
            amountCheckRunnable,
            actionWaitLengthMs
        )
    }

    /**
     * Scales up the elements after a delete if required.
     */
    private fun scaleUpIfRequired() {
        // check content width
        val first = elementContainerView.getFirstChild()!!
        val last = elementContainerView.getLastChild()!!
        var contentWidth = last.right - first.left

        // scale up if needed
        var scaleFactor = 1f
        while (contentWidth < elementContainerView.width
            && (currentTextSize * scaleFactor) < elementTextSize
        ) {
            contentWidth = (contentWidth * 1.05f).toInt()
            if (contentWidth < elementContainerView.width) {
                scaleFactor *= 1.05f
            }
        }
        // normalize scale factor if needed
        if ((currentTextSize * scaleFactor) > elementTextSize) {
            currentTextSize = elementTextSize
            currentAmountGemSize = amountGemSize
            currentFirstElementMarginStart = firstElementMarginStart
        } else {
            currentTextSize *= scaleFactor
            currentAmountGemSize *= scaleFactor
            currentFirstElementMarginStart =
                (currentFirstElementMarginStart * scaleFactor).toInt()
        }

        // adjust gem size
        UiUtil.setWidthAndHeight(
            amountGemImageView,
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        UiUtil.setStartMargin(
            elements[0].second,
            currentFirstElementMarginStart
        )
        // set center correction view width
        UiUtil.setWidth(
            amountCenterCorrectionView,
            currentAmountGemSize.toInt() + currentFirstElementMarginStart
        )
        // set text sizes
        for (element in elements) {
            element.second.setTextSizePx(currentTextSize)
            element.second.setWidthAndHeightToMeasured()
        }
    }

    private fun displayAvailableBalanceError() {
        // hide continue button
        continueButton.visibility = View.INVISIBLE

        // show validation message box
        if (notEnoughBalanceView.visibility == View.VISIBLE) {
            // return if visible already
            return
        }
        // don't allow digit entry during this animation
        digitAnimIsRunning = true
        notEnoughBalanceView.alpha = 0f
        notEnoughBalanceView.visibility = View.VISIBLE
        val viewAnim = ValueAnimator.ofFloat(0f, 1f)
        viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            notEnoughBalanceView.alpha = value
            val scale = value.remap(0f, 1f, 0.5f, 1f)
            notEnoughBalanceView.scaleX = scale
            notEnoughBalanceView.scaleY = scale
            // nudge the amount
            if (value < 0.5f) { // nudge right for the half of the animation
                elementContainerView.translationX = validationErrorNudgeDistance * value
            } else { // nudge back to original position for the second half
                elementContainerView.translationX = validationErrorNudgeDistance * (1f - value)
            }
            if (value == 1f) {
                digitAnimIsRunning = false
            }
        }
        viewAnim.duration = Constants.UI.shortDurationMs
        // define interpolator
        viewAnim.interpolator = EasingInterpolator(Ease.CIRC_OUT)
        viewAnim.start()
    }

    @OnClick(R.id.add_amount_btn_continue)
    fun continueButtonClicked() {
        continueButton.isClickable = false
        AsyncTask.execute {
            wr.get()?.checkActualAvailableBalance()
        }
    }

    private fun checkActualAvailableBalance() {
        val error = WalletError()
        val balanceInfo = walletService.getBalanceInfo(error)
        if (error.code == WalletErrorCode.NO_ERROR) {
            if (currentAmount > balanceInfo.availableBalance) {
                rootView.post {
                    wr.get()?.actualBalanceExceeded()
                }
            } else {
                rootView.post {
                    wr.get()?.continueToNote()
                    wr.get()?.continueButton?.isClickable = true
                }
            }
        } else {
            // TODO handle wallet error
            rootView.post {
                wr.get()?.continueButton?.isClickable = true
            }
        }
    }

    private fun actualBalanceExceeded() {
        listenerWR.get()?.onAmountExceedsActualAvailableBalance(this)
        continueButton.isClickable = true
    }

    private fun continueToNote() {
        val fee = WalletUtil.calculateTxFee()
        listenerWR.get()?.continueToNote(
            this,
            recipientUser,
            currentAmount,
            fee
        )
    }

    private fun showContinueButtonAnimated() {
        if (continueButton.visibility == View.VISIBLE) {
            return
        }
        continueButton.alpha = 0f
        continueButton.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(continueButton, "alpha", 0f, 1f)
        anim.duration = Constants.UI.shortDurationMs
        anim.start()
    }

    /**
     * Checks/validates the amount entered.
     */
    private inner class AmountCheckRunnable : Runnable {

        @SuppressLint("SetTextI18n")
        override fun run() {
            val error = WalletError()
            val balanceInfo = walletService.getBalanceInfo(error)
            if (error.code != WalletErrorCode.NO_ERROR) {
                TODO("Unhandled wallet error: ${error.code}")
            }
            // update fee
            val fee = WalletUtil.calculateTxFee()
            txFeeTextView.text = "+${WalletUtil.feeFormatter.format(fee.tariValue)}"
            // check balance
            val availableBalance =
                balanceInfo.availableBalance + balanceInfo.pendingIncomingBalance
            if ((currentAmount + WalletUtil.calculateTxFee()) > availableBalance) {
                availableBalanceTextView.text =
                    WalletUtil.amountFormatter.format(availableBalance.tariValue)
                wr.get()?.displayAvailableBalanceError()
                if (txFeeContainerView.visibility == View.INVISIBLE) {
                    txFeeContainerView.alpha = 0f
                    txFeeContainerView.visibility = View.VISIBLE
                    val viewAnim = ValueAnimator.ofFloat(0f, 1f)
                    viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
                        val value = valueAnimator.animatedValue as Float
                        wr.get()?.txFeeContainerView?.translationY = (1f - value) * 100
                        wr.get()?.txFeeContainerView?.alpha = value
                    }
                    viewAnim.duration = Constants.UI.shortDurationMs
                    // define interpolator
                    viewAnim.interpolator = EasingInterpolator(Ease.CIRC_OUT)
                    viewAnim.start()
                }
            } else {
                var showsTxFee = false
                var hidesTxFee = false
                // show/hide continue button
                if (currentAmount.value.toInt() == 0) {
                    continueButton.visibility = View.INVISIBLE
                    // hide fee
                    if (txFeeContainerView.visibility != View.INVISIBLE) {
                        hidesTxFee = true
                    }
                } else {
                    showContinueButtonAnimated()
                    // display fee
                    if (txFeeContainerView.visibility != View.VISIBLE) {
                        showsTxFee = true
                        txFeeContainerView.alpha = 0f
                        txFeeContainerView.visibility = View.VISIBLE
                    }
                }

                val viewAnim = ValueAnimator.ofFloat(0f, 1f)
                viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
                    val value = valueAnimator.animatedValue as Float
                    if (wr.get()?.notEnoughBalanceView?.visibility != View.INVISIBLE) {
                        // update validation view
                        wr.get()?.notEnoughBalanceView?.alpha = (1 - value)
                        val scale = (1 - value).remap(0f, 1f, 0.5f, 1f)
                        wr.get()?.notEnoughBalanceView?.scaleX = scale
                        wr.get()?.notEnoughBalanceView?.scaleY = scale
                        if (value == 1f) {
                            wr.get()?.notEnoughBalanceView?.visibility = View.INVISIBLE
                        }
                    }

                    // update tx fee view
                    if (showsTxFee) {
                        wr.get()?.txFeeContainerView?.translationY = (1f - value) * 100
                        wr.get()?.txFeeContainerView?.alpha = value
                    } else if (hidesTxFee) {
                        wr.get()?.txFeeContainerView?.translationY = value * 100
                        wr.get()?.txFeeContainerView?.alpha = (1f - value)
                    }
                    if (value == 1f && hidesTxFee) {
                        wr.get()?.txFeeContainerView?.visibility = View.INVISIBLE
                    }
                }
                viewAnim.duration = Constants.UI.shortDurationMs
                // define interpolator
                viewAnim.interpolator = EasingInterpolator(Ease.CIRC_OUT)
                viewAnim.start()
            }
        }
    }

    // region listener interface

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment)
        /**
         * Recipient is user.
         */
        fun continueToNote(
            sourceFragment: AddAmountFragment,
            recipientUser: User,
            amount: MicroTari,
            fee: MicroTari
        )

    }

    // endregion

}