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
package com.tari.android.wallet.ui.screen.send.addNote

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.dimen.add_note_slide_button_left_margin
import com.tari.android.wallet.R.dimen.add_note_slide_button_width
import com.tari.android.wallet.data.contacts.model.FFIContactInfo
import com.tari.android.wallet.databinding.FragmentAddNoteBinding
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_TRANSACTION
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.extension.dimenPx
import com.tari.android.wallet.util.extension.getStartMargin
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.hideKeyboard
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.parcelable
import com.tari.android.wallet.util.extension.postDelayed
import com.tari.android.wallet.util.extension.setStartMargin
import com.tari.android.wallet.util.extension.temporarilyDisableClick
import com.tari.android.wallet.util.extension.visible

class AddNoteFragment : CommonXmlFragment<FragmentAddNoteBinding, AddNoteViewModel>(), View.OnTouchListener {

    // slide button animation related variables
    private var slideButtonXDelta = 0
    private var slideButtonLastMarginStart = 0
    private var slideButtonContainerWidth = 0

    // Tx properties.
    private lateinit var transactionData: TransactionData
    private lateinit var recipientUser: FFIContactInfo
    private lateinit var amount: MicroTari
    private lateinit var note: String
    private var isOneSidePayment: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentAddNoteBinding.inflate(inflater, container, false).also { ui = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AddNoteViewModel by viewModels()
        bindViewModel(viewModel)

        retrievePageArguments()
        setupUI()
        setupCTAs()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        displayAliasOrEmojiId()
        ui.progressBar.setWhite()
        ui.noteEditText.addTextChangedListener(afterTextChanged = { updateSliderState() })
        ui.slideView.setOnTouchListener(this)
        // disable "send" slider
        disableCallToAction()
        focusEditTextAndShowKeyboard()
        ui.promptTextView.setTextColor(PaletteManager.getTextHeading(requireContext()))
        ui.noteEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.noteEditText.setText(transactionData.note.orEmpty())
        updateSliderState()
    }

    private fun updateSliderState() {
        if (ui.noteEditText.text?.toString().isNullOrEmpty()) {
            ui.promptTextView.setTextColor(PaletteManager.getTextHeading(requireContext()))
            disableCallToAction()
        } else {
            ui.promptTextView.setTextColor(PaletteManager.getTextBody(requireContext()))
            enableCallToAction()
        }
    }

    private fun retrievePageArguments() {
        transactionData = requireArguments().parcelable(PARAMETER_TRANSACTION)!!
        recipientUser = transactionData.recipientContact
        amount = transactionData.amount
        isOneSidePayment = transactionData.isOneSidePayment
        note = transactionData.note.orEmpty()
    }

    private fun setupCTAs() {
        ui.backCtaView.backPressedAction = { onBackButtonClicked() }
        ui.emojiIdSummaryContainerView.setOnClickListener { viewModel.emojiIdClicked(recipientUser.walletAddress) }
    }

    private fun displayAliasOrEmojiId() {
        val alias = recipientUser.getAlias()
        if (alias.isEmpty()) displayEmojiId(recipientUser.walletAddress) else displayAlias(alias)
    }

    private fun displayAlias(alias: String) {
        ui.emojiIdSummaryContainerView.gone()
        ui.titleTextView.visible()
        ui.titleTextView.text = alias
    }

    private fun displayEmojiId(address: TariWalletAddress) {
        ui.emojiIdSummaryContainerView.visible()
        ui.emojiIdViewContainer.textViewEmojiPrefix.text = address.addressPrefixEmojis()
        ui.emojiIdViewContainer.textViewEmojiFirstPart.text = address.addressFirstEmojis()
        ui.emojiIdViewContainer.textViewEmojiLastPart.text = address.addressLastEmojis()
        ui.titleTextView.gone()
    }

    private fun onBackButtonClicked() {
        ui.backCtaView.temporarilyDisableClick()
        // going back before hiding keyboard causes a blank white area on the screen
        // wait a while, then forward the back action to the host activity
        activity?.let {
            it.hideKeyboard()
            ui.rootView.postDelayed(Constants.UI.shortDurationMs, it.onBackPressedDispatcher::onBackPressed)
        }
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.noteEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(ui.noteEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableCallToAction() {
        if (ui.slideEnabledBgView.isVisible) {
            return
        }
        ui.slideView.setOnTouchListener(this)

        ui.slideToSendDisabledTextView.invisible()

        ui.slideToSendEnabledTextView.alpha = 0f
        ui.slideToSendEnabledTextView.visible()
        ui.slideToSendArrowEnabledImageView.alpha = 0f
        ui.slideToSendArrowEnabledImageView.visible()
        ui.slideEnabledBgView.alpha = 0f
        ui.slideEnabledBgView.visible()

        val textViewAnim = ObjectAnimator.ofFloat(ui.slideToSendEnabledTextView, "alpha", 0f, 1f).also { animations.add(it) }
        val arrowAnim = ObjectAnimator.ofFloat(ui.slideToSendArrowEnabledImageView, "alpha", 0f, 1f).also { animations.add(it) }
        val bgViewAnim = ObjectAnimator.ofFloat(ui.slideEnabledBgView, "alpha", 0f, 1f).also { animations.add(it) }

        // the animation set
        val animSet = AnimatorSet()
        animSet.playTogether(textViewAnim, arrowAnim, bgViewAnim)
        animSet.duration = Constants.UI.shortDurationMs
        animSet.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun disableCallToAction() {
        ui.slideToSendDisabledTextView.visible()
        ui.slideEnabledBgView.gone()
        ui.slideToSendEnabledTextView.gone()
        ui.slideToSendArrowEnabledImageView.gone()
        ui.slideView.setOnTouchListener(null)
    }

    /**
     * Controls the slide button animation & behaviour on drag.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val x = event.rawX.toInt()
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> Unit
            MotionEvent.ACTION_DOWN -> {
                slideButtonContainerWidth = ui.slideButtonContainerView.width
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                slideButtonXDelta = x - layoutParams.marginStart
            }

            MotionEvent.ACTION_MOVE -> {
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                val newLeftMargin = x - slideButtonXDelta
                slideButtonLastMarginStart = if (newLeftMargin < dimenPx(add_note_slide_button_left_margin)) {
                    dimenPx(add_note_slide_button_left_margin)
                } else {
                    if (newLeftMargin + dimenPx(add_note_slide_button_width) + dimenPx(add_note_slide_button_left_margin) >= slideButtonContainerWidth) {
                        slideButtonContainerWidth - dimenPx(add_note_slide_button_width) - dimenPx(add_note_slide_button_left_margin)
                    } else {
                        x - slideButtonXDelta
                    }
                }
                layoutParams.marginStart = slideButtonLastMarginStart
                val alpha = 1f - slideButtonLastMarginStart.toFloat() / (slideButtonContainerWidth -
                        dimenPx(add_note_slide_button_left_margin) -
                        dimenPx(add_note_slide_button_width))
                ui.slideToSendEnabledTextView.alpha = alpha
                ui.slideToSendDisabledTextView.alpha = alpha

                view.layoutParams = layoutParams
            }

            MotionEvent.ACTION_UP -> if (slideButtonLastMarginStart < slideButtonContainerWidth / 2) {
                animations += ValueAnimator.ofInt(slideButtonLastMarginStart, dimenPx(add_note_slide_button_left_margin)).apply {
                    addUpdateListener { valueAnimator: ValueAnimator ->
                        val margin = valueAnimator.animatedValue as Int
                        ui.slideView.setStartMargin(margin)
                        ui.slideToSendEnabledTextView.alpha = 1f - margin.toFloat() / (slideButtonContainerWidth
                                - dimenPx(add_note_slide_button_left_margin) - dimenPx(add_note_slide_button_width))
                    }
                    duration = Constants.UI.shortDurationMs
                    interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                    startDelay = 0
                    start()
                }
            } else {
                // disable input
                ui.noteEditText.isEnabled = false
                // complete slide animation
                animations += ValueAnimator.ofInt(
                    slideButtonLastMarginStart, slideButtonContainerWidth - dimenPx(add_note_slide_button_left_margin)
                            - dimenPx(add_note_slide_button_width)
                ).apply {
                    addUpdateListener { valueAnimator: ValueAnimator ->
                        val margin = valueAnimator.animatedValue as Int
                        ui.slideView.setStartMargin(margin)
                        ui.slideToSendEnabledTextView.alpha = 1f - margin.toFloat() / (slideButtonContainerWidth -
                                dimenPx(add_note_slide_button_left_margin) -
                                dimenPx(add_note_slide_button_width))
                    }
                    duration = Constants.UI.shortDurationMs
                    interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                    startDelay = 0
                    addListener(onEnd = { slideAnimationCompleted() })
                    start()
                }
            }
        }
        return false
    }

    private fun slideAnimationCompleted() {
        // hide slide view
        animations += ValueAnimator.ofFloat(1F, 0F).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                ui.slideView.alpha = valueAnimator.animatedValue as Float
            }
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = 0
            addListener(onEnd = {
                onSlideAnimationEnd()
                removeAllListeners()
            })
            start()
        }
    }

    private fun onSlideAnimationEnd() {
        hideKeyboard()
        if (!viewModel.isNetworkConnectionAvailable()) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) {
                restoreSlider()
                ui.noteEditText.isEnabled = true
                dialogHandler.showInternetConnectionErrorDialog()
            }
        } else {
            ui.progressBar.visible()
            ui.slideView.gone()
            continueToFinalizeSendTx()
        }
    }

    private fun hideKeyboard() {
        activity?.hideKeyboard()
        ui.noteEditText.clearFocus()
    }

    private fun continueToFinalizeSendTx() {
        // notify listener (i.e. activity)
        val note = ui.noteEditText.editableText.toString()
        val newData = transactionData.copy(note = note)

        viewModel.continueToFinalizeSendTx(newData)
    }

    private fun restoreSlider() {
        // hide slide view
        val slideViewInitialMargin = ui.slideView.getStartMargin()
        val slideViewMarginDelta = dimenPx(add_note_slide_button_left_margin) - slideViewInitialMargin
        animations += ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.slideView.alpha = 1f - value
                ui.slideToSendEnabledTextView.alpha = 1f - value
                ui.slideView.setStartMargin((slideViewInitialMargin + slideViewMarginDelta * (1 - value)).toInt())
            }
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            start()
        }
    }

    companion object {
        fun newInstance(transactionData: TransactionData) = AddNoteFragment().apply {
            arguments = Bundle().apply {
                putParcelable(PARAMETER_TRANSACTION, transactionData)
            }
        }
    }
}
