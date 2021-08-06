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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.view.doOnNextLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.enums.MediaType
import com.giphy.sdk.ui.pagination.GPHContent
import com.giphy.sdk.ui.views.GPHGridCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.DialogChooseGifBinding
import com.tari.android.wallet.databinding.FragmentAddNoteBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.adapter.GIFThumbnailAdapter
import com.tari.android.wallet.ui.fragment.send.repository.GiphyKeywordsRepository
import com.tari.android.wallet.ui.presentation.TxNote
import com.tari.android.wallet.ui.presentation.gif.GIF
import com.tari.android.wallet.ui.presentation.gif.GIFRepository
import com.tari.android.wallet.ui.presentation.gif.Placeholder
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.AddNoteAndSend
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import io.reactivex.Observer as RxObserver

/**
 * Add a note to the transaction & send it through this fragment.
 *
 * @author The Tari Development Team
 */
class AddNoteFragment : Fragment(), View.OnTouchListener {

    @Inject
    lateinit var tracker: Tracker

    private lateinit var listenerWR: WeakReference<Listener>

    // slide button animation related variables
    private var slideButtonXDelta = 0
    private var slideButtonLastMarginStart = 0
    private var slideButtonContainerWidth = 0

    // Formats the summarized emoji id.
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    // Control full emoji popups
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    // Tx properties.
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari

    private lateinit var ui: FragmentAddNoteBinding
    private lateinit var viewModel: ThumbnailGIFsViewModel
    private lateinit var gifContainer: GIFContainer
    private lateinit var adapter: GIFThumbnailAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
        listenerWR = WeakReference(context as Listener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddNoteBinding.inflate(inflater, container, false).also { ui = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_note", title = "Send Tari - Add Note")
        }
        initializeGIFsViewModel()
        retrievePageArguments(savedInstanceState)
        setupUI(savedInstanceState)
        setupCTAs()
    }

    private fun initializeGIFsViewModel() {
        viewModel = ViewModelProvider(this)[ThumbnailGIFsViewModel::class.java]
        viewModel.state.observe(viewLifecycleOwner) {
            when {
                it.isSuccessful -> adapter.repopulate(it.gifs!!)
                it.isError -> Logger.e("GIFs request had error")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GIF) {
            changeScrollViewBottomConstraint(R.id.slide_button_container_view)
            val media =
                data?.getParcelableExtra<Media>(ChooseGIFDialogFragment.MEDIA_DELIVERY_KEY)
                    ?: return
            gifContainer.gif = media.let {
                GIF(it.id, Uri.parse(it.embedUrl), Uri.parse(it.images.original!!.gifUrl))
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
        gifContainer = GIFContainer(
            Glide.with(this),
            ui.gifContainerView,
            ui.gifImageView,
            ui.searchGiphyContainerView,
            state
        )
        if (gifContainer.gif != null) changeScrollViewBottomConstraint(R.id.slide_button_container_view)
        adapter = GIFThumbnailAdapter(Glide.with(this), ::handleViewMoreGIFsIntent) {
            if (gifContainer.isShown) {
                changeScrollViewBottomConstraint(R.id.slide_button_container_view)
                gifContainer.gif = it
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
        fullEmojiIdViewController.fullEmojiId = recipientUser.publicKey.emojiId
        fullEmojiIdViewController.emojiIdHex = recipientUser.publicKey.hexString

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
        if (ui.noteEditText.text?.toString().isNullOrEmpty() && gifContainer.gif == null) {
            ui.promptTextView.setTextColor(color(black))
            disableCallToAction()
        } else {
            ui.promptTextView.setTextColor(color(add_note_prompt_passive_color))
            enableCallToAction()
        }
    }

    private fun retrievePageArguments(savedInstanceState: Bundle?) {
        recipientUser = requireArguments().getParcelable("recipientUser")!!
        amount = requireArguments().getParcelable("amount")!!
        if (savedInstanceState == null) {
            requireArguments().getString(DeepLink.PARAMETER_NOTE)
                ?.let { ui.noteEditText.setText(it) }
        }
    }

    private fun setupCTAs() {
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.removeGifCtaView.setOnClickListener {
            changeScrollViewBottomConstraint(R.id.search_giphy_container_view)
            gifContainer.gif = null
            updateSliderState()
        }
        ui.searchGiphyCtaView.setOnClickListener {
            handleViewMoreGIFsIntent()
        }
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
            displayEmojiId(recipientUser.publicKey.emojiId)
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
            ui.rootView.postDelayed(Constants.UI.shortDurationMs, it::onBackPressed)
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
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
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
                    val margin = valueAnimator.animatedValue as Int
                    ui.slideView.setStartMargin(margin)
                    ui.slideToSendEnabledTextView.alpha =
                        1f - margin.toFloat() / (slideButtonContainerWidth - dimenPx(
                            add_note_slide_button_left_margin
                        ) - dimenPx(add_note_slide_button_width))
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.start()
            } else {
                // disable input
                ui.noteEditText.isEnabled = false
                // complete slide animation
                val anim = ValueAnimator.ofInt(
                    slideButtonLastMarginStart,
                    slideButtonContainerWidth - dimenPx(add_note_slide_button_left_margin) - dimenPx(
                        add_note_slide_button_width
                    )
                )
                anim.addUpdateListener { valueAnimator: ValueAnimator ->
                    val margin = valueAnimator.animatedValue as Int
                    ui.slideView.setStartMargin(margin)
                    ui.slideToSendEnabledTextView.alpha =
                        1f - margin.toFloat() / (slideButtonContainerWidth -
                                dimenPx(add_note_slide_button_left_margin) -
                                dimenPx(add_note_slide_button_width))
                }
                anim.duration = Constants.UI.shortDurationMs
                anim.interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
                anim.startDelay = 0
                anim.addListener(onEnd = { slideAnimationCompleted() })
                anim.start()
            }
        }
        return false
    }

    private fun slideAnimationCompleted() {
        // hide slide view
        val anim = ValueAnimator.ofFloat(1F, 0F)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            ui.slideView.alpha = valueAnimator.animatedValue as Float
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
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs) { hideKeyboard() }
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs + Constants.UI.keyboardHideWaitMs) {
                restoreSlider()
                ui.noteEditText.isEnabled = true
                showInternetConnectionErrorDialog(requireActivity())
            }
        } else {
            ui.removeGifCtaView.isEnabled = false
            ui.progressBar.visible()
            ui.slideView.gone()
            ui.rootView.postDelayed(AddNoteAndSend.preKeyboardHideWaitMs) { hideKeyboard() }
            val totalTime =
                AddNoteAndSend.preKeyboardHideWaitMs + AddNoteAndSend.continueToFinalizeSendTxDelayMs
            ui.rootView.postDelayed(totalTime) { continueToFinalizeSendTx() }
        }
    }

    private fun hideKeyboard() {
        activity?.hideKeyboard()
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
            TxNote(
                ui.noteEditText.editableText.toString(),
                gifContainer.gif?.embedUri?.toString()
            ).compose()
        )
    }

    private fun restoreSlider() {
        // hide slide view
        val slideViewInitialMargin = ui.slideView.getStartMargin()
        val slideViewMarginDelta =
            dimenPx(add_note_slide_button_left_margin) - slideViewInitialMargin
        val anim = ValueAnimator.ofFloat(
            1f,
            0f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.slideView.alpha = 1f - value
            ui.slideToSendEnabledTextView.alpha = 1f - value
            ui.slideView.setStartMargin(
                (slideViewInitialMargin + slideViewMarginDelta * (1 - value)).toInt()
            )
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
            note: String
        )

    }

    private inner class GIFContainer(
        private val glide: RequestManager,
        private val gifContainerView: View,
        private val gifView: ImageView,
        thumbnailsContainer: View,
        state: Bundle?
    ) {

        private val transformation = RequestOptions().transform(RoundedCorners(10))
        private var animation = GIFsPanelAnimation(thumbnailsContainer)

        val isShown: Boolean
            get() = animation.isViewShown

        var gif: GIF? = null
            set(value) {
                field = value
                if (value == null) {
                    glide.clear(gifView)
                    showContainer()
                } else {
                    showGIF()
                    glide.asGif()
                        .placeholder(Placeholder.color(value).asDrawable())
                        .apply(transformation)
                        .load(value.uri)
                        .transition(DrawableTransitionOptions.withCrossFade(250))
                        .into(gifView)
                }
            }

        init {
            gif = state?.getParcelable(KEY_GIF)
        }

        private fun showContainer() {
            animation.show()
            gifContainerView.gone()
        }

        private fun showGIF() {
            animation.hide()
            gifContainerView.visible()
        }

        fun save(bundle: Bundle) {
            gif?.run { bundle.putParcelable(KEY_GIF, this) }
        }

        fun dispose() {
            animation.dispose()
        }

    }

    private data class GIFsPanelAnimationState(
        val direction: TranslationDirection,
        val animator: Animator?
    )

    private enum class TranslationDirection { UP, DOWN }

    private class GIFsPanelAnimation(private val view: View) {
        private var state =
            GIFsPanelAnimationState(TranslationDirection.UP, null)
        val isViewShown
            get() = state.direction == TranslationDirection.UP

        fun show() {
            state.animator?.cancel()
            state = createState(TranslationDirection.UP, to = 0F)
        }

        fun hide() {
            state.animator?.cancel()
            state = createState(TranslationDirection.DOWN, to = view.height.toFloat())
        }

        private fun createState(direction: TranslationDirection, to: Float) =
            GIFsPanelAnimationState(
                direction,
                ValueAnimator.ofFloat(view.translationY, to).apply {
                    duration = TRANSLATION_DURATION
                    addUpdateListener {
                        view.translationY = it.animatedValue as Float
                    }
                    start()
                })

        fun dispose() {
            this.state.animator?.cancel()
        }

        private companion object {
            private const val TRANSLATION_DURATION = 300L
        }
    }

    class ThumbnailGIFsViewModel() : CommonViewModel() {

        init {
            component?.inject(this)
        }

        @Inject
        lateinit var gifsRepository: GIFRepository

        @Inject
        lateinit var giphyKeywordsRepository: GiphyKeywordsRepository


        private val _state = MutableLiveData<GIFsState>()
        val state: LiveData<GIFsState> get() = _state

        init {
            fetchGIFs()
        }

        private fun fetchGIFs() {
            viewModelScope.launch(Dispatchers.Main) {
                _state.value = GIFsState()
                try {
                    val gifs = withContext(Dispatchers.IO) {
                        gifsRepository.getAll(
                            giphyKeywordsRepository.getNext(),
                            THUMBNAIL_REQUEST_LIMIT
                        )
                    }
                    _state.value = GIFsState(gifs)
                } catch (e: Exception) {
                    Logger.e(e, "Error occurred while fetching gifs")
                    _state.value = GIFsState(e)
                }
            }
        }

        class GIFsState private constructor(val gifs: List<GIF>?, val error: Exception?) {
            // Loading state
            constructor() : this(null, null)
            constructor(gifs: List<GIF>) : this(gifs, null)
            constructor(e: Exception) : this(null, e)

            val isError get() = error != null
            val isSuccessful get() = gifs != null
        }
    }

    private class HorizontalInnerMarginDecoration(private val value: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            if (parent.getChildLayoutPosition(view) > 0) outRect.left = value
        }
    }

    class ChooseGIFDialogFragment @Deprecated(
        """Use newInstance() and supply all the necessary data via arguments instead, as fragment's 
default no-op constructor is used by the framework for UI tree rebuild on configuration changes"""
    ) constructor() : DialogFragment() {
        private lateinit var ui: DialogChooseGifBinding
        private lateinit var behavior: BottomSheetBehavior<View>
        private lateinit var searchSubscription: Disposable

        @Inject
        lateinit var giphyKeywordsRepository: GiphyKeywordsRepository

        override fun onAttach(context: Context) {
            super.onAttach(context)
            appComponent.inject(this)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = DialogChooseGifBinding.inflate(inflater, container, false).also { ui = it }.root

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val searchSubject = BehaviorSubject.create<String>()
            searchSubscription = searchSubject
                .debounce(500L, TimeUnit.MILLISECONDS)
                .map { if (it.isEmpty()) giphyKeywordsRepository.getCurrent() else it }
                .map { GPHContent.searchQuery(it, mediaType = MediaType.gif) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { ui.giphyGridView.content = it }
            setupUI(searchSubject)
        }

        override fun onDestroyView() {
            searchSubscription.dispose()
            super.onDestroyView()
        }

        private fun setupUI(observer: RxObserver<String>) {
            ui.giphyGridView.content =
                GPHContent.searchQuery(giphyKeywordsRepository.getCurrent(), MediaType.gif)
            ui.giphyGridView.callback = object : GPHGridCallback {
                override fun contentDidUpdate(resultCount: Int) {
                    // No-op
                }

                override fun didSelectMedia(media: Media) {
                    val intent = Intent().apply { putExtra(MEDIA_DELIVERY_KEY, media) }
                    targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                    behavior.state = STATE_HIDDEN
                }
            }
            ui.gifSearchEditText.addTextChangedListener(
                afterTextChanged = afterChanged@{
                    observer.onNext(it?.toString() ?: return@afterChanged)
                }
            )
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            BottomSheetDialog(requireContext(), R.style.ChooseGIFDialog)
                .apply { setOnShowListener { setupDialog(this) } }

        private fun setupDialog(bottomSheetDialog: BottomSheetDialog) {
            val bottomSheet: View = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)!!
            behavior = BottomSheetBehavior<View>()
            behavior.isHideable = true
            behavior.skipCollapsed = true
            behavior.state = STATE_HIDDEN
            val layoutParams = bottomSheet.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.behavior = behavior
            bottomSheetDialog.setOnKeyListener { _, keyCode, _ ->
                (keyCode == KeyEvent.KEYCODE_BACK && behavior.state != STATE_HIDDEN &&
                        behavior.state != STATE_COLLAPSED).also {
                    if (it) behavior.state = STATE_HIDDEN
                }
            }
            ui.root.doOnNextLayout {
                behavior.state = STATE_EXPANDED
                behavior.addCallback(
                    onStateChange = { _, state ->
                        if (state == STATE_HIDDEN || state == STATE_COLLAPSED) dismiss()
                    },
                    onSlided = { _, slideOffset ->
                        val alpha = (slideOffset.coerceIn(0F, 1F) * 255).toInt()
                        val color = Color.argb(alpha, 0, 0, 0)
                        (bottomSheet.parent as View).setBackgroundColor(color)
                    }
                )
            }
        }

        private fun BottomSheetBehavior<*>.addCallback(
            onStateChange: (View, Int) -> Unit = { _, _ -> },
            onSlided: (View, Float) -> Unit = { _, _ -> },
        ) = addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) =
                onStateChange(bottomSheet, newState)

            override fun onSlide(bottomSheet: View, slideOffset: Float) =
                onSlided(bottomSheet, slideOffset)
        })

        companion object {
            @Suppress("DEPRECATION")
            fun newInstance() = ChooseGIFDialogFragment()
            const val MEDIA_DELIVERY_KEY = "key_media"
        }
    }

    private companion object {
        private const val KEY_GIF = "keygif"
        private const val REQUEST_CODE_GIF = 1535
        private const val THUMBNAIL_REQUEST_LIMIT = 20
    }
}


