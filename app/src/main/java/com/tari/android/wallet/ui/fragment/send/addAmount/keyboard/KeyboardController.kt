package com.tari.android.wallet.ui.fragment.send.addAmount.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewInputAmountBinding
import com.tari.android.wallet.databinding.ViewNumpadBinding
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.extension.dimen
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.getFirstChild
import com.tari.android.wallet.ui.extension.getLastChild
import com.tari.android.wallet.ui.extension.setLayoutSize
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setStartMargin
import com.tari.android.wallet.ui.extension.setTextSizePx
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setWidthAndHeightToMeasured
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.application.walletManager.WalletFileUtil
import java.math.BigInteger
import kotlin.math.min

class KeyboardController {

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

    private val decimalSeparator = WalletFileUtil.amountFormatter.decimalFormatSymbols.decimalSeparator.toString()
    private val thousandsSeparator = WalletFileUtil.amountFormatter.decimalFormatSymbols.groupingSeparator.toString()

    /**
     * Below two are related to amount check and validation.
     */
    private val amountCheckHandler = Handler(Looper.getMainLooper())
    private lateinit var amountCheckRunnable: Runnable
    private lateinit var context: Context
    private lateinit var numpadBinding: ViewNumpadBinding
    private lateinit var amountInputBinding: ViewInputAmountBinding
    private var startAmount: Double = Double.MIN_VALUE

    private var isFirstLaunch: Boolean = true

    fun setup(
        context: Context,
        amountRunnable: Runnable,
        numpadBinding: ViewNumpadBinding,
        amountInputBinding: ViewInputAmountBinding,
        startAmount: Double? = Double.MIN_VALUE
    ) {
        this.context = context
        this.numpadBinding = numpadBinding
        this.amountInputBinding = amountInputBinding
        this.amountCheckRunnable = amountRunnable
        this.startAmount = startAmount ?: Double.MIN_VALUE
        setupUI()
    }

    private fun setupUI() {
        numpadBinding.decimalPointButton.text = decimalSeparator
        currentTextSize = context.dimen(R.dimen.add_amount_element_text_size)
        currentAmountGemSize = context.dimen(R.dimen.add_amount_gem_size)
        currentFirstElementMarginStart = context.dimenPx(R.dimen.add_amount_leftmost_digit_margin_start)
        // setup amount validation
        amountCheckRunnable.run()
        // add first digit to the element list
        elements.clear()
        elements.add(Pair("0", amountInputBinding.amountElement0TextView))

        // input start amount
        if (isFirstLaunch && startAmount != Double.MIN_VALUE) {
            val handler = Handler(Looper.getMainLooper())
            isFirstLaunch = false
            handler.post {
                startAmount.toString().withIndex().forEach { (index, char) ->
                    if (Character.isDigit(char)) {
                        onDigitOrSeparatorClicked(char.toString())
                    } else {
                        onDigitOrSeparatorClicked(decimalSeparator)
                    }
                }
            }
            handler.postDelayed(
                this::setActionBindings,
                (startAmount.toString().length + 1) * Constants.UI.AddAmount.numPadDigitEnterAnimDurationMs * 2
            )
        } else {
            setActionBindings()
        }
    }

    private fun setActionBindings() = with(numpadBinding) {
        deleteButton.setOnClickListener { deleteButtonClicked() }
        pad0Button.setOnClickListener { onDigitOrSeparatorClicked("0") }
        pad1Button.setOnClickListener { onDigitOrSeparatorClicked("1") }
        pad2Button.setOnClickListener { onDigitOrSeparatorClicked("2") }
        pad3Button.setOnClickListener { onDigitOrSeparatorClicked("3") }
        pad4Button.setOnClickListener { onDigitOrSeparatorClicked("4") }
        pad5Button.setOnClickListener { onDigitOrSeparatorClicked("5") }
        pad6Button.setOnClickListener { onDigitOrSeparatorClicked("6") }
        pad7Button.setOnClickListener { onDigitOrSeparatorClicked("7") }
        pad8Button.setOnClickListener { onDigitOrSeparatorClicked("8") }
        pad9Button.setOnClickListener { onDigitOrSeparatorClicked("9") }
        decimalPointButton.setOnClickListener { onDigitOrSeparatorClicked(decimalSeparator) }
    }

    /**
     * -1 if no decimal separator, index otherwise.
     */
    private val decimalSeparatorIndex: Int
        get() = elements.map { it.first }.indexOf(decimalSeparator)


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
                amountInputBinding.elementContainerView.removeView(element.second)
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
     * Calculates the current amount in micro tari.
     */
    val currentAmount: MicroTari
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
            amountInputBinding.elementContainerView.getLastChild()!!.right - amountInputBinding.elementContainerView.getFirstChild()!!.left
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
            elements[0] = elements[0].copy(first = digit)
            elements[0].second
        } else { // inflate text view
            val textView = inflater.inflate(
                R.layout.view_add_amount_element,
                amountInputBinding.elementContainerView,
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
            amountInputBinding.elementContainerView.addView(textView, amountInputBinding.elementContainerView.childCount - 1)
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
            R.layout.view_add_amount_element,
            amountInputBinding.elementContainerView,
            false
        ) as TextView
        textView.setTextSizePx(currentTextSize)
        textView.setWidthAndHeightToMeasured()
        textView.text = thousandsSeparator
        // add child - add 1 for the gem icon (it's the very first child)
        amountInputBinding.elementContainerView.addView(textView, index + 1)
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
        while ((contentWidthPostInsert * scaleFactor) > amountInputBinding.elementContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor
        currentFirstElementMarginStart =
            (currentFirstElementMarginStart * scaleFactor).toInt()

        // adjust gem size
        amountInputBinding.amountGemImageView.setLayoutSize(
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        elements[0].second.setStartMargin(currentFirstElementMarginStart)
        // set center correction view width
        val width = currentAmountGemSize.toInt() + currentFirstElementMarginStart
        amountInputBinding.amountCenterCorrectionView.setLayoutWidth(width)
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
            R.layout.view_add_amount_element,
            amountInputBinding.root.parent as ViewGroup,
            false
        ) as TextView?
        ghostTextView!!.text = digit
        ghostTextView.setWidthAndHeightToMeasured()
        ghostTextView.alpha = 0f
        (amountInputBinding.root.parent as ViewGroup).addView(ghostTextView)

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
                amountInputBinding.elementContainerView.top
                        + context.dimenPx(R.dimen.add_amount_element_container_translation_y)
                        + (amountInputBinding.elementContainerView.height * (1 - value) * 0.8f).toInt()
            )
            ghostTextView!!.alpha = value

            if (value == 1f) {
                if (enteringFirstDigit) {
                    textView.text = digit
                    textView.setWidthAndHeightToMeasured()
                }
                textView.alpha = 1f
                (amountInputBinding.root.parent as ViewGroup).removeView(ghostTextView)
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
        amountInputBinding.elementContainerView.removeView(removed.second)
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
        val first = amountInputBinding.elementContainerView.getFirstChild()!!
        val last = amountInputBinding.elementContainerView.getLastChild()!!
        var contentWidth = last.right - first.left

        // scale up if needed
        var scaleFactor = 1f
        while (contentWidth < amountInputBinding.elementContainerView.width
            && (currentTextSize * scaleFactor) < context.dimen(R.dimen.add_amount_element_text_size)
        ) {
            contentWidth = (contentWidth * 1.05f).toInt()
            if (contentWidth < amountInputBinding.elementContainerView.width) {
                scaleFactor *= 1.05f
            }
        }
        // normalize scale factor if needed
        if ((currentTextSize * scaleFactor) > context.dimen(R.dimen.add_amount_element_text_size)) {
            currentTextSize = context.dimen(R.dimen.add_amount_element_text_size)
            currentAmountGemSize = context.dimen(R.dimen.add_amount_gem_size)
            currentFirstElementMarginStart = context.dimenPx(R.dimen.add_amount_leftmost_digit_margin_start)
        } else {
            currentTextSize *= scaleFactor
            currentAmountGemSize *= scaleFactor
            currentFirstElementMarginStart =
                (currentFirstElementMarginStart * scaleFactor).toInt()
        }

        // adjust gem size
        amountInputBinding.amountGemImageView.setLayoutSize(
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        // adjust first element margin
        elements[0].second.setStartMargin(
            currentFirstElementMarginStart
        )
        // set center correction view width
        val width = currentAmountGemSize.toInt() + currentFirstElementMarginStart
        amountInputBinding.amountCenterCorrectionView.setLayoutWidth(width)
        // set text sizes
        for (element in elements) {
            element.second.setTextSizePx(currentTextSize)
            element.second.setWidthAndHeightToMeasured()
        }
    }

    fun nudgeAmountView(): ValueAnimator = with(amountInputBinding) {
        // don't allow digit entry during this animation
        digitAnimIsRunning = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                // nudge the amount
                if (value < 0.5f) { // nudge right for the half of the animation
                    elementContainerView.translationX =
                        context.dimenPx(R.dimen.add_amount_available_balance_error_amount_nudge_distance) * value
                } else { // nudge back to original position for the second half
                    elementContainerView.translationX =
                        context.dimenPx(R.dimen.add_amount_available_balance_error_amount_nudge_distance) * (1f - value)
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
}

