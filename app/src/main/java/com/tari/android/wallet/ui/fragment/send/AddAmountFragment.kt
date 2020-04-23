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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentAddAmountBinding
import com.tari.android.wallet.extension.remap
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.WalletUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.ref.WeakReference
import java.math.BigInteger
import javax.inject.Inject
import kotlin.math.min

/**
 * Amount entry fragment.
 *
 * @author The Tari Development Team
 */
class AddAmountFragment(private val walletService: TariWalletService) : Fragment() {

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

    @BindColor(R.color.black)
    @JvmField
    var blackColor = 0

    @BindColor(R.color.light_gray)
    @JvmField
    var lightGrayColor = 0

    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator)
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

    private var _ui: FragmentAddAmountBinding? = null
    private val ui get() = _ui!!

    companion object {

        // TODO [CRASH]
        fun newInstance(walletService: TariWalletService): AddAmountFragment {
            return AddAmountFragment(walletService)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAddAmountBinding.inflate(inflater, container, false).also { _ui = it }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AddAmountFragmentVisitor.visit(this, view)
        setupUi()

        TrackHelper.track()
            .screen("/home/send_tari/add_amount")
            .title("Send Tari - Add Amount")
            .with(tracker)
    }

    private fun setupUi() {
        recipientUser = arguments!!.getParcelable("recipientUser")!!
        ui.decimalPointButton.text = decimalSeparator
        currentTextSize = elementTextSize
        currentAmountGemSize = amountGemSize
        currentFirstElementMarginStart = firstElementMarginStart
        // hide validation
        ui.notEnoughBalanceView.invisible()
        // hide tx fee
        ui.txFeeContainerView.invisible()
        // hide/disable continue button
        ui.continueButton.invisible()
        ui.disabledContinueButton.visible()
        // add first digit to the element list
        elements.add(Pair("0", ui.amountElement0TextView))
        ui.fullEmojiIdBgClickBlockerView.isClickable = false
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        displayAliasOrEmojiId()
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        hideFullEmojiId(animated = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)
        ui.rootView.doOnGlobalLayout {
            UiUtil.setTopMargin(ui.txFeeContainerView, ui.elementContainerView.bottom)
            UiUtil.setTopMargin(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.top)
            UiUtil.setHeight(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.height)
            UiUtil.setWidth(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.width)
        }
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.dimmerView.setOnClickListener { onEmojiIdDimmerClicked() }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked(it) }
        ui.txFeeDescTextView.setOnClickListener { onFeeViewClick() }
        ui.deleteButton.setOnClickListener { deleteButtonClicked() }
        ui.continueButton.setOnClickListener { continueButtonClicked() }
        ui.pad0Button.setOnClickListener { onDigitOrSeparatorClicked("0") }
        ui.pad1Button.setOnClickListener { onDigitOrSeparatorClicked("1") }
        ui.pad2Button.setOnClickListener { onDigitOrSeparatorClicked("2") }
        ui.pad3Button.setOnClickListener { onDigitOrSeparatorClicked("3") }
        ui.pad4Button.setOnClickListener { onDigitOrSeparatorClicked("4") }
        ui.pad5Button.setOnClickListener { onDigitOrSeparatorClicked("5") }
        ui.pad6Button.setOnClickListener { onDigitOrSeparatorClicked("6") }
        ui.pad7Button.setOnClickListener { onDigitOrSeparatorClicked("7") }
        ui.pad8Button.setOnClickListener { onDigitOrSeparatorClicked("8") }
        ui.pad9Button.setOnClickListener { onDigitOrSeparatorClicked("9") }
        ui.decimalPointButton.setOnClickListener { onDigitOrSeparatorClicked(decimalSeparator) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    private fun displayAliasOrEmojiId() {
        if (recipientUser is Contact) {
            ui.emojiIdSummaryContainerView.gone()
            ui.titleTextView.visible()
            ui.titleTextView.text = (recipientUser as Contact).alias
        } else {
            displayEmojiId(recipientUser.publicKey.emojiId)
        }
    }

    private fun displayEmojiId(emojiId: String) {
        ui.emojiIdSummaryContainerView.visible()
        emojiIdSummaryController.display(emojiId)
        ui.titleTextView.gone()
        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId,
            emojiIdChunkSeparator,
            blackColor,
            lightGrayColor
        )
    }

    override fun onDetach() {
        amountCheckHandler.removeCallbacks(amountCheckRunnable)
        super.onDetach()
    }

    private fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val mActivity = activity ?: return
        mActivity.onBackPressed()
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    private fun emojiIdClicked() {
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        ui.fullEmojiIdBgClickBlockerView.isClickable = true
        // make dimmers non-clickable until the anim is over
        ui.dimmerView.isClickable = false
        // prepare views
        ui.emojiIdSummaryContainerView.invisible()
        ui.dimmerView.alpha = 0f
        ui.dimmerView.visible()
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth =
            (ui.rootView.width - horizontalMargin * 2) - fullEmojiIdInitialWidth
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
        ui.copyEmojiIdButtonContainerView.alpha = 0f
        ui.copyEmojiIdButtonContainerView.visible()
        UiUtil.setBottomMargin(
            ui.copyEmojiIdButtonContainerView,
            0
        )
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.dimmerView.alpha = value * 0.6f
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = value
            ui.fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            ui.fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            ui.backButton.alpha = 1 - value
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdButtonContainerView,
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
                ui.dimmerView.isClickable = true
            }
        })
        // scroll animation
        ui.fullEmojiIdScrollView.postDelayed({
            ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true, animated: Boolean) {
        if (!animated) {
            ui.fullEmojiIdContainerView.gone()
            ui.dimmerView.gone()
            ui.copyEmojiIdButtonContainerView.gone()
            return
        }
        ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        ui.emojiIdSummaryContainerView.visible()
        ui.emojiIdSummaryContainerView.alpha = 0f
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdButtonContainerView.alpha = value
            UiUtil.setBottomMargin(
                ui.copyEmojiIdButtonContainerView,
                (copyEmojiIdButtonVisibleBottomMargin * value).toInt()
            )
        }
        // emoji id anim
        val fullEmojiIdInitialWidth = ui.fullEmojiIdContainerView.width
        val fullEmojiIdDeltaWidth =
            ui.emojiIdSummaryContainerView.width - ui.fullEmojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.dimmerView.alpha = (1 - value) * 0.6f
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            ui.emojiIdSummaryContainerView.alpha = value
            ui.backButton.alpha = value
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
                ui.dimmerView.gone()
                ui.fullEmojiIdBgClickBlockerView.isClickable = false
                ui.fullEmojiIdContainerView.gone()
                ui.copyEmojiIdButtonContainerView.gone()
            }
        })
    }

    /**
     * Dimmer clicked.
     */
    private fun onEmojiIdDimmerClicked() {
        hideFullEmojiId(animated = true)
    }

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        ui.dimmerView.isClickable = false
        val mActivity = activity ?: return
        val clipBoard = ContextCompat.getSystemService(mActivity, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            recipientUser.publicKey.emojiId
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false, animated = true)
        }
        // hide copy emoji id button
        val copyEmojiIdButtonAnim = ui.copyEmojiIdButtonContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    private fun onFeeViewClick() {
        showTxFeeToolTip()
    }

    private fun showTxFeeToolTip() {
        BottomSlideDialog(
            context = activity ?: return,
            layoutId = R.layout.tx_fee_tooltip_dialog,
            dismissViewId = R.id.tx_fee_tooltip_dialog_txt_close
        ).show()
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
                ui.elementContainerView.removeView(element.second)
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
    private fun onDigitOrSeparatorClicked(digit: String) {
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
            ui.elementContainerView.getLastChild()!!.right - ui.elementContainerView.getFirstChild()!!.left
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
                ui.elementContainerView,
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
            ui.elementContainerView.addView(textView, ui.elementContainerView.childCount - 1)
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
            ui.elementContainerView,
            false
        ) as TextView
        textView.setTextSizePx(currentTextSize)
        textView.setWidthAndHeightToMeasured()
        textView.text = thousandsSeparator
        // add child - add 1 for the gem icon (it's the very first child)
        ui.elementContainerView.addView(textView, index + 1)
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
        while ((contentWidthPostInsert * scaleFactor) > ui.elementContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor
        currentFirstElementMarginStart =
            (currentFirstElementMarginStart * scaleFactor).toInt()

        // adjust gem size
        UiUtil.setWidthAndHeight(
            ui.amountGemImageView,
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
            ui.amountCenterCorrectionView,
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
            ui.elementOuterContainerView,
            false
        ) as TextView?
        ghostTextView!!.text = digit
        ghostTextView.setWidthAndHeightToMeasured()
        ghostTextView.alpha = 0f
        ui.elementOuterContainerView.addView(ghostTextView)

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
                ui.elementContainerView.top
                        + amountElementContainerTranslationY
                        + (ui.elementContainerView.height * (1 - value) * 0.8f).toInt()
            )
            ghostTextView!!.alpha = value

            if (value == 1f) {
                if (enteringFirstDigit) {
                    textView.text = digit
                    textView.setWidthAndHeightToMeasured()
                }
                textView.alpha = 1f
                ui.elementOuterContainerView.removeView(ghostTextView)
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

    private fun deleteButtonClicked() {
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
        ui.elementContainerView.removeView(removed.second)
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
        val first = ui.elementContainerView.getFirstChild()!!
        val last = ui.elementContainerView.getLastChild()!!
        var contentWidth = last.right - first.left

        // scale up if needed
        var scaleFactor = 1f
        while (contentWidth < ui.elementContainerView.width
            && (currentTextSize * scaleFactor) < elementTextSize
        ) {
            contentWidth = (contentWidth * 1.05f).toInt()
            if (contentWidth < ui.elementContainerView.width) {
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
            ui.amountGemImageView,
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
            ui.amountCenterCorrectionView,
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
        ui.continueButton.invisible()

        // show validation message box
        if (ui.notEnoughBalanceView.visibility == View.VISIBLE) {
            // return if visible already
            return
        }
        // don't allow digit entry during this animation
        digitAnimIsRunning = true
        ui.notEnoughBalanceView.alpha = 0f
        ui.notEnoughBalanceView.visible()
        val viewAnim = ValueAnimator.ofFloat(0f, 1f)
        viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.notEnoughBalanceView.alpha = value
            val scale = value.remap(0f, 1f, 0.5f, 1f)
            ui.notEnoughBalanceView.scaleX = scale
            ui.notEnoughBalanceView.scaleY = scale
            // nudge the amount
            if (value < 0.5f) { // nudge right for the half of the animation
                ui.elementContainerView.translationX = validationErrorNudgeDistance * value
            } else { // nudge back to original position for the second half
                ui.elementContainerView.translationX = validationErrorNudgeDistance * (1f - value)
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

    private fun continueButtonClicked() {
        ui.continueButton.isClickable = false
        AsyncTask.execute {
            wr.get()?.checkActualAvailableBalance()
        }
    }

    private fun checkActualAvailableBalance() {
        val error = WalletError()
        val balanceInfo = walletService.getBalanceInfo(error)
        if (error.code == WalletErrorCode.NO_ERROR) {
            if (currentAmount > balanceInfo.availableBalance) {
                ui.rootView.post {
                    wr.get()?.actualBalanceExceeded()
                }
            } else {
                ui.rootView.post {
                    wr.get()?.continueToNote()
                    wr.get()?.ui?.continueButton?.isClickable = true
                }
            }
        } else {
            // TODO handle wallet error
            ui.rootView.post {
                wr.get()?.ui?.continueButton?.isClickable = true
            }
        }
    }

    private fun actualBalanceExceeded() {
        listenerWR.get()?.onAmountExceedsActualAvailableBalance(this)
        ui.continueButton.isClickable = true
    }

    private fun continueToNote() {
        val fee = WalletUtil.calculateTxFee()
        listenerWR.get()?.continueToAddNote(
            this,
            recipientUser,
            currentAmount,
            fee
        )
    }

    private fun showContinueButtonAnimated() {
        if (ui.continueButton.visibility == View.VISIBLE) {
            return
        }
        ui.continueButton.alpha = 0f
        ui.continueButton.visible()
        val anim = ObjectAnimator.ofFloat(ui.continueButton, "alpha", 0f, 1f)
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
            ui.txFeeTextView.text = "+${WalletUtil.feeFormatter.format(fee.tariValue)}"
            // check balance
            val availableBalance =
                balanceInfo.availableBalance + balanceInfo.pendingIncomingBalance
            if ((currentAmount + WalletUtil.calculateTxFee()) > availableBalance) {
                ui.availableBalanceTextView.text =
                    WalletUtil.amountFormatter.format(availableBalance.tariValue)
                wr.get()?.displayAvailableBalanceError()
                if (ui.txFeeContainerView.visibility == View.INVISIBLE) {
                    ui.txFeeContainerView.alpha = 0f
                    ui.txFeeContainerView.visible()
                    val viewAnim = ValueAnimator.ofFloat(0f, 1f)
                    viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
                        val value = valueAnimator.animatedValue as Float
                        wr.get()?.ui?.txFeeContainerView?.translationY = (1f - value) * 100
                        wr.get()?.ui?.txFeeContainerView?.alpha = value
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
                    ui.continueButton.invisible()
                    // hide fee
                    if (ui.txFeeContainerView.visibility != View.INVISIBLE) {
                        hidesTxFee = true
                    }
                } else {
                    showContinueButtonAnimated()
                    // display fee
                    if (ui.txFeeContainerView.visibility != View.VISIBLE) {
                        showsTxFee = true
                        ui.txFeeContainerView.alpha = 0f
                        ui.txFeeContainerView.visible()
                    }
                }

                val viewAnim = ValueAnimator.ofFloat(0f, 1f)
                viewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
                    val value = valueAnimator.animatedValue as Float
                    if (wr.get()?.ui?.notEnoughBalanceView?.visibility != View.INVISIBLE) {
                        // update validation view
                        wr.get()?.ui?.notEnoughBalanceView?.alpha = (1 - value)
                        val scale = (1 - value).remap(0f, 1f, 0.5f, 1f)
                        wr.get()?.ui?.notEnoughBalanceView?.scaleX = scale
                        wr.get()?.ui?.notEnoughBalanceView?.scaleY = scale
                        if (value == 1f) {
                            wr.get()?.ui?.notEnoughBalanceView?.invisible()
                        }
                    }

                    // update tx fee view
                    if (showsTxFee) {
                        wr.get()?.ui?.txFeeContainerView?.translationY = (1f - value) * 100
                        wr.get()?.ui?.txFeeContainerView?.alpha = value
                    } else if (hidesTxFee) {
                        wr.get()?.ui?.txFeeContainerView?.translationY = value * 100
                        wr.get()?.ui?.txFeeContainerView?.alpha = (1f - value)
                    }
                    if (value == 1f && hidesTxFee) {
                        wr.get()?.ui?.txFeeContainerView?.invisible()
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
        fun continueToAddNote(
            sourceFragment: AddAmountFragment,
            recipientUser: User,
            amount: MicroTari,
            fee: MicroTari
        )

    }

    // endregion

    private object AddAmountFragmentVisitor {
        internal fun visit(fragment: AddAmountFragment, view: View) {
            fragment.requireActivity().appComponent.inject(fragment)
            ButterKnife.bind(fragment, view)
        }
    }

}
