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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.emoji_id_chunk_separator
import com.tari.android.wallet.databinding.FragmentAddNoteBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Add a note to the transaction & send it through this fragment.
 *
 * @author The Tari Development Team
 */
class AddNoteFragment : Fragment(), TextWatcher, View.OnTouchListener {

    @Inject
    lateinit var tracker: Tracker

    private lateinit var listenerWR: WeakReference<Listener>

    // slide button animation related variables
    private var slideButtonXDelta = 0
    private var slideButtonLastMarginStart = 0
    private var slideButtonContainerWidth = 0

    /**
     * Formats the summarized emoji id.
     */
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    /**
     * Animates the emoji id "copied" text.
     */
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    /**
     * Tx properties.
     */
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari
    private lateinit var fee: MicroTari

    private var _ui: FragmentAddNoteBinding? = null
    private val ui get() = _ui!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAddNoteBinding.inflate(inflater, container, false).also { _ui = it }.root

    override fun onDestroyView() {
        super.onDestroyView()
        ui.noteEditText.removeTextChangedListener(this)
        _ui = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AddNoteFragmentVisitor.visit(this)
        // get tx properties
        recipientUser = arguments!!.getParcelable("recipientUser")!!
        amount = arguments!!.getParcelable("amount")!!
        fee = arguments!!.getParcelable("fee")!!
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        ui.fullEmojiIdBgClickBlockerView.isClickable = false
        ui.fullEmojiIdContainerView.gone()
        displayAliasOrEmojiId()
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        hideFullEmojiId(animated = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)

        UiUtil.setProgressBarColor(ui.progressBar, color(white))

        ui.noteEditText.addTextChangedListener(this)
        ui.slideView.setOnTouchListener(this)

        // disable "send" slider
        disableCallToAction()
        focusEditTextAndShowKeyboard()

        ui.promptTextView.setTextColor(color(black))
        ui.noteEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.noteEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)

        ui.rootView.doOnGlobalLayout {
            UiUtil.setTopMargin(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.top)
            UiUtil.setHeight(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.height)
            UiUtil.setWidth(ui.fullEmojiIdContainerView, ui.emojiIdSummaryContainerView.width)
        }

        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.dimmerView.setOnClickListener { onEmojiIdDimmerClicked() }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked(it) }
        ui.copyEmojiIdButton.setOnLongClickListener { copyEmojiIdButton ->
            onCopyEmojiIdButtonLongClicked(copyEmojiIdButton)
            true
        }

        tracker.screen(path = "/home/send_tari/add_note", title = "Send Tari - Add Note")
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
            string(emoji_id_chunk_separator),
            color(black),
            color(light_gray)
        )
    }

    private fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        // going back before hiding keyboard causes a blank white area on the screen
        // wait a while, then forward the back action to the host activity
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        ui.rootView.postDelayed({
            mActivity.onBackPressed()
        }, Constants.UI.shortDurationMs)
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
            (ui.rootView.width - dimenPx(common_horizontal_margin) * 2) - fullEmojiIdInitialWidth
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
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim, copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(onEnd = { ui.dimmerView.isClickable = true })
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
                (dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
            )
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
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
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(onEnd = {
            ui.dimmerView.gone()
            ui.fullEmojiIdBgClickBlockerView.isClickable = false
            ui.fullEmojiIdContainerView.gone()
            ui.copyEmojiIdButtonContainerView.gone()
        })
    }

    /**
     * Dimmer clicked - hide dimmers.
     */
    private fun onEmojiIdDimmerClicked() {
        hideFullEmojiId(animated = true)
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.noteEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    private fun completeCopyEmojiId(clipboardString: String) {
        ui.dimmerView.isClickable = false
        val mActivity = activity ?: return
        val clipBoard = ContextCompat.getSystemService(mActivity, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Identity",
            clipboardString
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

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(recipientUser.publicKey.emojiId)
    }

    private fun onCopyEmojiIdButtonLongClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopyEmojiId(recipientUser.publicKey.hexString)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableCallToAction() {
        if (ui.slideEnabledBgView.visibility == View.VISIBLE) {
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

        val textViewAnim = ObjectAnimator.ofFloat(ui.slideToSendEnabledTextView, "alpha", 0f, 1f)
        val arrowAnim = ObjectAnimator.ofFloat(ui.slideToSendArrowEnabledImageView, "alpha", 0f, 1f)
        val bgViewAnim = ObjectAnimator.ofFloat(ui.slideEnabledBgView, "alpha", 0f, 1f)

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

    // region text change listener

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable) {
        if (s.toString().isNotEmpty()) {
            ui.promptTextView.setTextColor(color(add_note_prompt_passive_color))
            enableCallToAction()
        } else {
            ui.promptTextView.setTextColor(color(black))
            disableCallToAction()
        }
    }

    // endregion

    /**
     * Controls the slide button animation & behaviour on drag.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        view: View,
        event: MotionEvent
    ): Boolean {
        val x = event.rawX.toInt()
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                slideButtonContainerWidth = ui.slideButtonContainerView.width
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                slideButtonXDelta = x - layoutParams.marginStart
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                val newLeftMargin = x - slideButtonXDelta
                slideButtonLastMarginStart = if (newLeftMargin < dimenPx(
                        add_note_slide_button_left_margin
                    )
                ) {
                    dimenPx(add_note_slide_button_left_margin)
                } else {
                    if (newLeftMargin + dimenPx(add_note_slide_button_width) + dimenPx(
                            add_note_slide_button_left_margin
                        ) >= slideButtonContainerWidth
                    ) {
                        slideButtonContainerWidth - dimenPx(add_note_slide_button_width) - dimenPx(
                            add_note_slide_button_left_margin
                        )
                    } else {
                        x - slideButtonXDelta
                    }
                }
                layoutParams.marginStart = slideButtonLastMarginStart
                val alpha =
                    1f - slideButtonLastMarginStart.toFloat() /
                            (slideButtonContainerWidth -
                                    dimenPx(add_note_slide_button_left_margin) -
                                    dimenPx(add_note_slide_button_width))
                ui.slideToSendEnabledTextView.alpha = alpha
                ui.slideToSendDisabledTextView.alpha = alpha

                view.layoutParams = layoutParams
            }
            MotionEvent.ACTION_UP -> if (slideButtonLastMarginStart < slideButtonContainerWidth / 2) {
                val anim = ValueAnimator.ofInt(
                    slideButtonLastMarginStart,
                    dimenPx(add_note_slide_button_left_margin)
                )
                anim.addUpdateListener { valueAnimator: ValueAnimator ->
                    _ui?.let { ui ->
                        val margin = valueAnimator.animatedValue as Int
                        UiUtil.setStartMargin(ui.slideView, margin)
                        ui.slideToSendEnabledTextView.alpha =
                            1f - margin.toFloat() / (slideButtonContainerWidth - dimenPx(
                                add_note_slide_button_left_margin
                            ) - dimenPx(add_note_slide_button_width))
                    }
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.start()
            } else {
                // disable input
                ui.noteEditText.inputType = InputType.TYPE_NULL
                // complete slide animation
                val anim = ValueAnimator.ofInt(
                    slideButtonLastMarginStart,
                    slideButtonContainerWidth - dimenPx(add_note_slide_button_left_margin) - dimenPx(
                        add_note_slide_button_width
                    )
                )
                anim.addUpdateListener { valueAnimator: ValueAnimator ->
                    _ui?.let { ui ->
                        val margin = valueAnimator.animatedValue as Int
                        UiUtil.setStartMargin(ui.slideView, margin)
                        ui.slideToSendEnabledTextView.alpha =
                            1f - margin.toFloat() / (slideButtonContainerWidth -
                                    dimenPx(add_note_slide_button_left_margin) -
                                    dimenPx(add_note_slide_button_width))
                    }
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.addListener(onEnd = { _ui?.let { slideAnimationCompleted() } })
                anim.start()
            }
        }
        return false
    }

    private fun slideAnimationCompleted() {
        // hide slide view
        val anim = ValueAnimator.ofFloat(1F, 0F)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            _ui?.slideView?.alpha = valueAnimator.animatedValue as Float
        }
        anim.duration = Constants.UI.shortDurationMs
        anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        anim.startDelay = 0
        anim.addListener(onEnd = {
            onSlideAnimationEnd()
            anim.removeAllListeners()
        })
        anim.start()
    }

    private fun onSlideAnimationEnd() {
        _ui?.let { ui ->
            if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
                ui.rootView.postDelayed({
                    hideKeyboard()
                }, Constants.UI.AddNoteAndSend.preKeyboardHideWaitMs)
                ui.rootView.postDelayed(
                    {
                        restoreSlider()
                        ui.noteEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
                        showInternetConnectionErrorDialog(activity!!)
                    },
                    Constants.UI.AddNoteAndSend.preKeyboardHideWaitMs + Constants.UI.keyboardHideWaitMs
                )
                return@onSlideAnimationEnd
            }
            ui.progressBar.visible()
            ui.slideView.gone()
            ui.rootView.postDelayed({
                hideKeyboard()
            }, Constants.UI.AddNoteAndSend.preKeyboardHideWaitMs)
            ui.rootView.postDelayed(
                {
                    continueToFinalizeSendTx()
                }, Constants.UI.AddNoteAndSend.preKeyboardHideWaitMs
                        + Constants.UI.AddNoteAndSend.continueToFinalizeSendTxDelayMs
            )
        }
    }

    private fun hideKeyboard() {
        UiUtil.hideKeyboard(activity ?: return)
        ui.noteEditText.clearFocus()
    }

    private fun continueToFinalizeSendTx() {
        // track event
        tracker.event(category = "Transaction", action = "Transaction Initiated")
        // notify listener (i.e. activity)
        listenerWR.get()?.continueToFinalizeSendTx(
            this,
            recipientUser,
            amount,
            fee,
            ui.noteEditText.editableText.toString()
        )
    }

    private fun restoreSlider() {
        // hide slide view
        val slideViewInitialMargin = UiUtil.getStartMargin(ui.slideView)
        val slideViewMarginDelta =
            dimenPx(add_note_slide_button_left_margin) - slideViewInitialMargin
        val anim = ValueAnimator.ofFloat(
            1f,
            0f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            _ui?.let { ui ->
                val value = valueAnimator.animatedValue as Float
                ui.slideView.alpha = 1f - value
                ui.slideToSendEnabledTextView.alpha = 1f - value
                UiUtil.setStartMargin(
                    ui.slideView,
                    (slideViewInitialMargin + slideViewMarginDelta * (1 - value)).toInt()
                )
            }
        }
        anim.duration = Constants.UI.shortDurationMs
        anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
        anim.start()
    }

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        fun continueToFinalizeSendTx(
            sourceFragment: AddNoteFragment,
            recipientUser: User,
            amount: MicroTari,
            fee: MicroTari,
            note: String
        )

    }

    private object AddNoteFragmentVisitor {
        internal fun visit(fragment: AddNoteFragment) {
            fragment.requireActivity().appComponent.inject(fragment)
        }
    }

}
