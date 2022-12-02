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
package com.tari.android.wallet.ui.fragment.send.addNote

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.addListener
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.giphy.sdk.core.models.Media
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.databinding.FragmentAddNoteBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.activity.SendTariActivity
import com.tari.android.wallet.ui.fragment.send.addNote.gif.*
import com.tari.android.wallet.ui.fragment.send.addNote.gif.ThumbnailGIFsViewModel.Companion.REQUEST_CODE_GIF
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

/**
 * Add a note to the transaction & send it through this fragment.
 *
 * @author The Tari Development Team
 */
class AddNoteFragment : Fragment(), View.OnTouchListener {

    private lateinit var addNodeListenerWR: WeakReference<AddNodeListener>

    // slide button animation related variables
    private var slideButtonXDelta = 0
    private var slideButtonLastMarginStart = 0
    private var slideButtonContainerWidth = 0

    // Formats the summarized emoji id.
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    // Control full emoji popups
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    // Tx properties.
    private lateinit var transactionData: TransactionData
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari
    private var isOneSidePayment: Boolean = false

    private lateinit var ui: FragmentAddNoteBinding
    private lateinit var viewModel: ThumbnailGIFsViewModel
    private lateinit var gifContainer: GIFContainer
    private lateinit var adapter: GIFThumbnailAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
        addNodeListenerWR = WeakReference(context as AddNodeListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentAddNoteBinding.inflate(inflater, container, false).also { ui = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeGIFsViewModel()
        retrievePageArguments(savedInstanceState)
        setupUI(savedInstanceState)
        setupCTAs()
    }

    private fun initializeGIFsViewModel() {
        viewModel = ViewModelProvider(this)[ThumbnailGIFsViewModel::class.java]
        observe(viewModel.state) {
            if (it.isSuccessful) {
                adapter.repopulate(it.gifItems!!)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GIF) {
            changeScrollViewBottomConstraint(R.id.slide_button_container_view)
            val media = data?.parcelable<Media>(ChooseGIFDialogFragment.MEDIA_DELIVERY_KEY) ?: return
            gifContainer.gifItem = media.let {
                GIFItem(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.original!!.gifUrl))
            }
            updateSliderState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        gifContainer.save(outState)
    }

    override fun onDestroyView() {
        gifContainer.dispose()
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(state: Bundle?) {
        gifContainer = GIFContainer(Glide.with(this), ui.gifContainerView, ui.gifImageView, ui.searchGiphyContainerView, state)
        if (gifContainer.gifItem != null) changeScrollViewBottomConstraint(R.id.slide_button_container_view)
        adapter = GIFThumbnailAdapter(Glide.with(this), ::handleViewMoreGIFsIntent) {
            if (gifContainer.isShown) {
                changeScrollViewBottomConstraint(R.id.slide_button_container_view)
                gifContainer.gifItem = it
                updateSliderState()
            }
        }
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)

        val fullEmojiIdListener = object : FullEmojiIdViewController.Listener {
            override fun animationHide(value: Float) {
                ui.backButton.alpha = 1 - value
            }

            override fun animationShow(value: Float) {
                ui.backButton.alpha = 1 - value
            }
        }

        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext(),
            fullEmojiIdListener
        )
        fullEmojiIdViewController.fullEmojiId = recipientUser.walletAddress.emojiId
        fullEmojiIdViewController.emojiIdHex = recipientUser.walletAddress.hexString

        displayAliasOrEmojiId()
        ui.progressBar.setColor(color(white))
        ui.noteEditText.addTextChangedListener(afterTextChanged = { updateSliderState() })
        ui.slideView.setOnTouchListener(this)
        // disable "send" slider
        disableCallToAction()
        focusEditTextAndShowKeyboard()
        ui.promptTextView.setTextColor(color(black))
        ui.noteEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        ui.thumbnailGifsRecyclerView.also {
            val margin = dimen(add_note_gif_inner_margin).toInt()
            it.addItemDecoration(HorizontalInnerMarginDecoration(margin))
            it.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            it.adapter = adapter
        }
    }

    private fun updateSliderState() {
        if (ui.noteEditText.text?.toString().isNullOrEmpty() && gifContainer.gifItem == null) {
            ui.promptTextView.setTextColor(color(black))
            disableCallToAction()
        } else {
            ui.promptTextView.setTextColor(color(add_note_prompt_passive_color))
            enableCallToAction()
        }
    }

    private fun retrievePageArguments(savedInstanceState: Bundle?) {
        transactionData = requireArguments().parcelable("transactionData")!!
        recipientUser = transactionData.recipientUser!!
        amount = transactionData.amount!!
        isOneSidePayment = transactionData.isOneSidePayment
        if (savedInstanceState == null) {
            requireArguments().getString(SendTariActivity.PARAMETER_NOTE)
                ?.let { ui.noteEditText.setText(it) }
        }
    }

    private fun setupCTAs() {
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.removeGifCtaView.setOnClickListener {
            changeScrollViewBottomConstraint(R.id.search_giphy_container_view)
            gifContainer.gifItem = null
            updateSliderState()
        }
        ui.searchGiphyCtaView.setOnClickListener { handleViewMoreGIFsIntent() }
    }

    private fun handleViewMoreGIFsIntent() {
        if (gifContainer.isShown) {
            requireActivity().hideKeyboard()
            ChooseGIFDialogFragment.newInstance()
                .apply { setTargetFragment(this@AddNoteFragment, REQUEST_CODE_GIF) }
                .show(requireActivity().supportFragmentManager, null)
        }

    }

    private fun changeScrollViewBottomConstraint(toTopOf: Int) {
        val set = ConstraintSet().apply { clone(ui.rootView) }
        set.connect(R.id.message_body_scroll_view, ConstraintSet.BOTTOM, toTopOf, ConstraintSet.TOP)
        set.applyTo(ui.rootView)
    }

    private fun displayAliasOrEmojiId() {
        if (recipientUser is Contact) {
            ui.emojiIdSummaryContainerView.gone()
            ui.titleTextView.visible()
            ui.titleTextView.text = (recipientUser as Contact).alias
        } else {
            displayEmojiId(recipientUser.walletAddress.emojiId)
        }
    }

    private fun displayEmojiId(emojiId: String) {
        ui.emojiIdSummaryContainerView.visible()
        emojiIdSummaryController.display(emojiId)
        ui.titleTextView.gone()
    }

    private fun onBackButtonClicked(view: View) {
        view.temporarilyDisableClick()
        // going back before hiding keyboard causes a blank white area on the screen
        // wait a while, then forward the back action to the host activity
        activity?.let {
            it.hideKeyboard()
            ui.rootView.postDelayed(Constants.UI.shortDurationMs, it.onBackPressedDispatcher::onBackPressed)
        }
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    private fun emojiIdClicked() {
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.noteEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(ui.noteEditText, InputMethodManager.HIDE_IMPLICIT_ONLY)
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
                ValueAnimator.ofInt(slideButtonLastMarginStart, dimenPx(add_note_slide_button_left_margin)).apply {
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
                ValueAnimator.ofInt(
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
        ValueAnimator.ofFloat(1F, 0F).apply {
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
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) {
                restoreSlider()
                ui.noteEditText.isEnabled = true
                showInternetConnectionErrorDialog(requireActivity())
            }
        } else {
            ui.removeGifCtaView.isEnabled = false
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
        val note = TxNote(ui.noteEditText.editableText.toString(), gifContainer.gifItem?.embedUri?.toString()).compose()
        val newData = transactionData.copy(note = note)
        addNodeListenerWR.get()?.continueToFinalizeSendTx(newData)
    }

    private fun restoreSlider() {
        // hide slide view
        val slideViewInitialMargin = ui.slideView.getStartMargin()
        val slideViewMarginDelta = dimenPx(add_note_slide_button_left_margin) - slideViewInitialMargin
        ValueAnimator.ofFloat(1f, 0f).apply {
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
}


