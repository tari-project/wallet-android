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
package com.tari.android.wallet.ui.fragment.send.addAmount

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.content.res.ColorStateList
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.FragmentAddAmountBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.math.BigInteger
import javax.inject.Inject
import kotlin.math.min

/**
 * Amount entry fragment.
 *
 * @author The Tari Development Team
 */
class AddAmountFragment : Fragment(), ServiceConnection {

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

    private lateinit var listenerWR: WeakReference<Listener>

    /**
     * Recipient is either an emoji id or a user from contacts or recent txs.
     */
    private lateinit var recipientUser: User

    /**
     *     Control full emoji popups
     */
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    /**
     * Formats the summarized emoji id.
     */
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    private lateinit var walletService: TariWalletService

    private var isFirstLaunch: Boolean = false
    private lateinit var ui: FragmentAddAmountBinding

    private var estimatedFee: MicroTari? = null
    private lateinit var balanceInfo: BalanceInfo
    private lateinit var availableBalance: MicroTari

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddAmountBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        bindToWalletService()
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_amount", title = "Send Tari - Add Amount")
        }
        isFirstLaunch = savedInstanceState == null
    }

    private fun bindToWalletService() {
        val bindIntent = Intent(requireActivity(), WalletService::class.java)
        requireActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("AddAmountFragment onServiceConnected")
        walletService = TariWalletService.Stub.asInterface(service)
        // Only binding UI if we have not passed `onDestroyView` line, which is a possibility
        setupUI()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("AddAmountFragment onServiceDisconnected")
        // No-op for now
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(this)
    }

    private fun setupUI() {
        recipientUser = requireArguments().getParcelable("recipientUser")!!
        ui.decimalPointButton.text = decimalSeparator
        currentTextSize = dimen(add_amount_element_text_size)
        currentAmountGemSize = dimen(add_amount_gem_size)
        currentFirstElementMarginStart = dimenPx(add_amount_leftmost_digit_margin_start)
        // setup amount validation
        amountCheckRunnable.run()
        // hide tx fee
        ui.txFeeContainerView.invisible()
        // hide/disable continue button
        ui.continueButton.invisible()
        ui.disabledContinueButton.visible()
        // add first digit to the element list
        elements.add(Pair("0", ui.amountElement0TextView))
        val fullEmojiIdListener = object : FullEmojiIdViewController.Listener {
            override fun animationHide(value: Float) {
                ui.backButton.alpha = 1 - value
            }

            override fun animationShow(value: Float) {
                ui.backButton.alpha = 1 - value
            }
        }
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext(),
            fullEmojiIdListener
        )
        fullEmojiIdViewController.fullEmojiId = recipientUser.publicKey.emojiId
        fullEmojiIdViewController.emojiIdHex = recipientUser.publicKey.hexString
        displayAliasOrEmojiId()
        val amount = requireArguments().getDouble(DeepLink.PARAMETER_AMOUNT, Double.MIN_VALUE)
        if (isFirstLaunch && amount != Double.MIN_VALUE) {
            val handler = Handler(Looper.getMainLooper())
            amount.toString().withIndex().forEach { (index, char) ->
                handler.postDelayed({
                    if (Character.isDigit(char)) {
                        onDigitOrSeparatorClicked(char.toString())
                    } else {
                        onDigitOrSeparatorClicked(decimalSeparator)
                    }
                }, (index + 1) * Constants.UI.AddAmount.numPadDigitEnterAnimDurationMs * 2)
            }
            handler.postDelayed(
                this::setActionBindings,
                (amount.toString().length + 1) * Constants.UI.AddAmount.numPadDigitEnterAnimDurationMs * 2
            )
        } else {
            setActionBindings()
        }
    }

    private fun setActionBindings() {
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
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
    }

    override fun onDetach() {
        amountCheckHandler.removeCallbacks(amountCheckRunnable)
        super.onDetach()
    }

    private fun onBackButtonClicked(view: View) {
        view.temporarilyDisableClick()
        val mActivity = activity ?: return
        mActivity.onBackPressed()
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    private fun emojiIdClicked() {
        fullEmojiIdViewController.showFullEmojiId()
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
        val onesEndIndex = if (decimalIndex >= 1) { // no thousands
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
                textView.setLayoutWidth(0)
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
        ui.amountGemImageView.setLayoutSize(
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        elements[0].second.setStartMargin(currentFirstElementMarginStart)
        // set center correction view width
        val width = currentAmountGemSize.toInt() + currentFirstElementMarginStart
        ui.amountCenterCorrectionView.setLayoutWidth(width)
        // set text sizes
        for (element in elements) {
            element.second.setTextSizePx(currentTextSize)
            if (enteringFirstDigit
                || enteringDecimalPoint
                || element !== elements.last()
            ) {
                element.second.setWidthAndHeightToMeasured()
            } else {
                textView.setLayoutWidth(0)
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
                textView.setLayoutWidth(width.toInt())
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
                ghostTextView!!.setStartMargin(
                    (location[0] + ((1 - value) * measuredWidth * 0.8f)).toInt()
                )
            } else {
                ghostTextView!!.setStartMargin(
                    (location[0] + ((1 - value) * measuredWidth * 0.3f)).toInt()
                )
            }
            ghostTextView!!.setTopMargin(
                ui.elementContainerView.top
                        + dimenPx(add_amount_element_container_translation_y)
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
            && (currentTextSize * scaleFactor) < dimen(add_amount_element_text_size)
        ) {
            contentWidth = (contentWidth * 1.05f).toInt()
            if (contentWidth < ui.elementContainerView.width) {
                scaleFactor *= 1.05f
            }
        }
        // normalize scale factor if needed
        if ((currentTextSize * scaleFactor) > dimen(add_amount_element_text_size)) {
            currentTextSize = dimen(add_amount_element_text_size)
            currentAmountGemSize = dimen(add_amount_gem_size)
            currentFirstElementMarginStart = dimenPx(add_amount_leftmost_digit_margin_start)
        } else {
            currentTextSize *= scaleFactor
            currentAmountGemSize *= scaleFactor
            currentFirstElementMarginStart =
                (currentFirstElementMarginStart * scaleFactor).toInt()
        }

        // adjust gem size
        ui.amountGemImageView.setLayoutSize(
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        elements[0].second.setStartMargin(
            currentFirstElementMarginStart
        )
        // set center correction view width
        val width = currentAmountGemSize.toInt() + currentFirstElementMarginStart
        ui.amountCenterCorrectionView.setLayoutWidth(width)
        // set text sizes
        for (element in elements) {
            element.second.setTextSizePx(currentTextSize)
            element.second.setWidthAndHeightToMeasured()
        }
    }

    private fun continueButtonClicked() {
        ui.continueButton.isClickable = false
        lifecycleScope.launch(Dispatchers.IO) { checkAmountAndFee() }
    }

    private fun checkAmountAndFee() {
        val error = WalletError()
        val balanceInfo = walletService.getBalanceInfo(error)
        val fee = estimatedFee
        val amount = currentAmount
        if (error.code == WalletErrorCode.NO_ERROR && fee != null) {
            if (amount > balanceInfo.availableBalance) {
                lifecycleScope.launch(Dispatchers.Main) {
                    actualBalanceExceeded()
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (fee > amount) {
                        ErrorDialog(
                            requireActivity(),
                            title = string(R.string.error_fee_more_than_amount_title),
                            description = string(R.string.error_fee_more_than_amount_description),
                            canceledOnTouchOutside = true
                        ).show()
                        ui.continueButton.isClickable = true
                    } else {
                        continueToNote()
                        ui.continueButton.isClickable = true
                    }
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                ui.continueButton.isClickable = true
            }
        }
    }

    private fun updateBalanceInfo() {
        val error = WalletError()

        balanceInfo = walletService.getBalanceInfo(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        availableBalance = balanceInfo.availableBalance + balanceInfo.pendingIncomingBalance

        ui.availableBalanceTextView.text =
            WalletUtil.balanceFormatter.format(availableBalance.tariValue)
    }

    private fun actualBalanceExceeded() {
        listenerWR.get()?.onAmountExceedsActualAvailableBalance(this)
        ui.continueButton.isClickable = true
    }

    private fun continueToNote() {
        listenerWR.get()?.continueToAddNote(this, recipientUser, currentAmount)
    }

    /**
     * Checks/validates the amount entered.
     */
    private inner class AmountCheckRunnable : Runnable {

        override fun run() {
            val error = WalletError()

            // update fee
            val fee = walletService.estimateTxFee(currentAmount, error)
            estimatedFee = fee

            if (error.code != WalletErrorCode.NO_ERROR
                && error.code != WalletErrorCode.NOT_ENOUGH_FUNDS
                && error.code != WalletErrorCode.FUNDS_PENDING
            ) {
                TODO("Unhandled wallet error: ${error.code}")
            }

            updateBalanceInfo()

            if (error.code == WalletErrorCode.FUNDS_PENDING
                || error.code == WalletErrorCode.NOT_ENOUGH_FUNDS
                || (currentAmount + fee) > availableBalance
            ) {
                showErrorState(error)
            } else {
                showSuccessState(fee)
            }
        }

        @SuppressLint("SetTextI18n")
        private fun showSuccessState(fee: MicroTari) = with(ui) {
            notEnoughBalanceDescriptionTextView.text = string(R.string.add_amount_wallet_balance)
            availableBalanceContainerView.visible()

            val showsTxFee: Boolean
            txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            // show/hide continue button
            if (currentAmount.value.toInt() == 0) {
                hideContinueButton()
                showsTxFee = false
            } else {
                showContinueButtonAnimated()
                showsTxFee = true
            }

            showBalance()

            showOrHideFeeViewAnimated(showsTxFee)
        }

        private fun showOrHideFeeViewAnimated(showsTxFee: Boolean) = with(ui) {
            if (showsTxFee && txFeeContainerView.visibility == View.VISIBLE ||
                !showsTxFee && txFeeContainerView.visibility == View.INVISIBLE
            ) {
                return@with
            }

            if (showsTxFee) {
                txFeeContainerView.alpha = 0f
                txFeeContainerView.visible()
            }

            ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener { valueAnimator: ValueAnimator ->
                    val value = valueAnimator.animatedValue as Float

                    if (showsTxFee) {
                        txFeeContainerView.translationY = (1f - value) * 100
                        txFeeContainerView.alpha = value
                    } else {
                        txFeeContainerView.translationY = value * 100
                        txFeeContainerView.alpha = (1f - value)

                        if (value == 1f) {
                            txFeeContainerView.invisible()
                        }
                    }
                }
                duration = Constants.UI.shortDurationMs
                interpolator = EasingInterpolator(Ease.CIRC_OUT)
                start()
            }
        }

        private fun showErrorState(
            error: WalletError
        ) = with(ui) {
            if (error.code == WalletErrorCode.FUNDS_PENDING) {
                availableBalanceContainerView.gone()
                notEnoughBalanceDescriptionTextView.text =
                    string(R.string.add_amount_funds_pending)
            } else {
                availableBalanceContainerView.visible()
                notEnoughBalanceDescriptionTextView.text =
                    string(R.string.add_amount_not_enough_available_balance)
            }

            hideContinueButton()

            showAvailableBalanceError()

            nudgeAmountView()

            showOrHideFeeViewAnimated(true)
        }

        private fun showAvailableBalanceError() = with(ui) {
            notEnoughBalanceView.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.validation_error_box_border_bg
            )
            gemNotEnoughBalance.imageTintList = null
            availableBalanceTextView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.common_error
                )
            )
        }

        private fun showBalance() = with(ui) {
            notEnoughBalanceView.background = null
            gemNotEnoughBalance.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), black))
            availableBalanceTextView.setTextColor(ContextCompat.getColor(requireContext(), black))
        }

        private fun nudgeAmountView() = with(ui) {
            // don't allow digit entry during this animation
            digitAnimIsRunning = true
            ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener { valueAnimator: ValueAnimator ->
                    val value = valueAnimator.animatedValue as Float
                    // nudge the amount
                    if (value < 0.5f) { // nudge right for the half of the animation
                        elementContainerView.translationX =
                            dimenPx(add_amount_available_balance_error_amount_nudge_distance) * value
                    } else { // nudge back to original position for the second half
                        elementContainerView.translationX =
                            dimenPx(add_amount_available_balance_error_amount_nudge_distance) * (1f - value)
                    }
                    if (value == 1f) {
                        digitAnimIsRunning = false
                    }
                }
                duration = Constants.UI.shortDurationMs
                interpolator = EasingInterpolator(Ease.CIRC_OUT)
                start()
            }
        }

        private fun showContinueButtonAnimated() = with(ui) {
            if (continueButton.visibility == View.VISIBLE) {
                return@with
            }
            continueButton.alpha = 0f
            continueButton.visible()
            ObjectAnimator.ofFloat(continueButton, "alpha", 0f, 1f).apply {
                duration = Constants.UI.shortDurationMs
                start()
            }
        }

        private fun hideContinueButton() = with(ui) {
            continueButton.invisible()
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
            amount: MicroTari
        )

    }

    // endregion

}
