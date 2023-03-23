package com.tari.android.wallet.ui.fragment.contact_book.contactSelection

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.databinding.FragmentContactsSelectionBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.setBottomMargin
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.ContactListAdapter
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatContactDto
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.firstNCharactersAreEmojis
import com.tari.android.wallet.util.numberOfEmojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

open class ContactSelectionFragment : CommonFragment<FragmentContactsSelectionBinding, ContactSelectionViewModel>(), TextWatcher {

    private val deeplinkViewModel: DeeplinkViewModel by viewModels()

    private var recyclerViewAdapter = ContactListAdapter()

    /**
     * Fields related to emoji id input masking.
     */
    private var isDeletingSeparatorAtIndex: Int? = null
    private var textWatcherIsRunning = false
    private var inputNormalLetterSpacing = 0.04f
    private var inputEmojiIdLetterSpacing = 0.27f

    private val textChangedProcessDelayMs = 100L
    private val textChangedProcessHandler = Handler(Looper.getMainLooper())
    private var textChangedProcessRunnable = Runnable { processTextChanged() }

    private var hidePasteEmojiIdViewsOnTextChanged = false
    private var yatEyeState = true

    private val dimmerViews
        get() = arrayOf(ui.middleDimmerView, ui.bottomDimmerView)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsSelectionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactSelectionViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        subscribeViewModal()
    }

    open fun goToNext() {
        requireActivity().hideKeyboard(ui.searchEditText)
        ui.searchEditText.clearFocus()
    }

    private fun subscribeViewModal() = with(viewModel) {
        observe(list) { recyclerViewAdapter.update(it) }

        observe(clipboardTariWalletAddress) { showClipboardData(it) }

        observe(selectedTariWalletAddress) { putEmojiId(it.emojiId) }

        observe(foundYatUser) { showYatUser(if (it.isPresent) it.get() else null) }

        observeOnLoad(clipboardChecker)
    }

    private fun putEmojiId(emojiId: String) {
        val rawEmojiId = emojiId.replace(string(R.string.emoji_id_chunk_separator), "")
        if (rawEmojiId == emojiId) return
        ui.searchEditText.setText(emojiId)
        ui.searchEditText.setSelectionToEnd()
    }

    private fun showClipboardData(data: TariWalletAddress) {
        ui.rootView.postDelayed({
            hidePasteEmojiIdViewsOnTextChanged = true
            showPasteEmojiIdViews(data.emojiId)
            focusEditTextAndShowKeyboard()
        }, 100)
    }

    private fun setupUI() {
        setupRecyclerView()
        ui.scrollDepthGradientView.alpha = 0f
        ui.toolbar.clearRightIcon()
        ui.invalidEmojiIdTextView.gone()
        hidePasteEmojiIdViews(animate = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.emojiIdScrollView)
        OverScrollDecoratorHelper.setUpOverScroll(ui.searchEditTextScrollView)
        ui.searchEditText.inputType = InputType.TYPE_NULL
        ui.qrCodeButton.setOnClickListener { onQRButtonClick(it) }
        ui.toolbar.rightAction = { goToNext() }
        dimmerViews.forEach { it.setOnClickListener { onEmojiIdDimmerClicked() } }
        ui.pasteEmojiIdButton.setOnClickListener { onPasteEmojiIdButtonClicked() }
        ui.emojiIdTextView.setOnClickListener { onPasteEmojiIdButtonClicked() }
        ui.yatEyeButton.setOnClickListener { toggleYatEye() }
        ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.searchEditText.addTextChangedListener(this@ContactSelectionFragment)
    }

    private fun setupRecyclerView() {
        ui.contactsListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener { onItemClick(it as? ContactItem) })
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
    }

    private fun onItemClick(contactItem: ContactItem?) {
        contactItem?.contact?.let { viewModel.selectedUser.value = it }
    }

    private fun toggleYatEye() {
        setYatState(!yatEyeState)
    }

    private fun setYatState(isOpen: Boolean) {
        if (!isOpen) {
            ui.searchEditText.removeTextChangedListener(this)
            ui.yatEyeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vector_closed_eye))
        } else {
            ui.searchEditText.removeTextChangedListener(this)
            ui.searchEditText.setSelectionToEnd()
            ui.searchEditText.addTextChangedListener(this)
            ui.yatEyeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vector_opened_eye))
        }
        yatEyeState = isOpen
    }

    private fun showYatUser(yatUser: YatContactDto?) {
        val isExist = yatUser != null
        ui.yatEyeButton.setVisible(isExist)
        ui.yatIcon.setVisible(isExist)

        if (isExist) {
            setYatState(true)
            ui.searchEditText.postDelayed({
                TransitionManager.beginDelayedTransition(ui.searchEditTextAnimateContainer)
                ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                ui.searchEditText.gravity = Gravity.CENTER
            }, 100)

            requireActivity().hideKeyboard()
            ui.searchEditText.clearFocus()
        } else {
            ui.searchEditText.postDelayed({
                ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                ui.searchEditText.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }, 100)
        }
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (ui.searchEditText.text.toString().isNotEmpty()) {
            ui.searchEditText.setText("")
            ui.toolbar.clearRightIcon()
        }
    }

    private fun startQRCodeActivity() {
        val intent = Intent(activity, QRScannerActivity::class.java)
        startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
        activity?.overridePendingTransition(R.anim.slide_up, 0)
    }


    /**
     * Displays paste-emoji-id-related views.
     */
    private fun showPasteEmojiIdViews(emojiId: String) {
        ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId,
            string(R.string.emoji_id_chunk_separator),
            viewModel.paletteManager.getBlack(requireContext()),
            viewModel.paletteManager.getLightGray(requireContext())
        )
        ui.emojiIdContainerView.setBottomMargin(-dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height))
        ui.emojiIdContainerView.visible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }

        // animate
        val emojiIdAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                dimmerViews.forEach { dimmerView -> dimmerView.alpha = animValue * 0.6f }
                ui.emojiIdContainerView.setBottomMargin((-dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height) * (1f - animValue)).toInt())
            }
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            duration = Constants.UI.mediumDurationMs
        }


        // animate and show paste emoji id button
        ui.pasteEmojiIdContainerView.setTopMargin(0)
        val pasteButtonAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (dimenPx(R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin) * value).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = value
            }
            addListener(onStart = { ui.pasteEmojiIdContainerView.visible() })
            interpolator = EasingInterpolator(Ease.BACK_OUT)
            duration = Constants.UI.shortDurationMs
        }

        AnimatorSet().apply {
            playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
            startDelay = Constants.UI.xShortDurationMs
            start()
        }
    }

    /**
     * Hides paste-emoji-id-related views.
     */
    private fun hidePasteEmojiIdViews(animate: Boolean, onEnd: (() -> (Unit))? = null) {
        if (!animate) {
            ui.pasteEmojiIdContainerView.gone()
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
            onEnd?.let { it() }
            return
        }
        // animate and hide paste emoji id button
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (dimenPx(R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin) * (1 - value)).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = (1 - value)
            }
            addListener(onEnd = { ui.pasteEmojiIdContainerView.gone() })
            duration = Constants.UI.shortDurationMs
        }
        // animate and hide emoji id & dimmers
        val emojiIdDisappearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                dimmerViews.forEach { dimmerView -> dimmerView.alpha = 0.6f * (1 - value) }
                ui.emojiIdContainerView.setBottomMargin((-dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height) * value).toInt())
            }
            addListener(onEnd = {
                ui.emojiIdContainerView.gone()
                dimmerViews.forEach(View::gone)
            })
            duration = Constants.UI.shortDurationMs
        }

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(pasteButtonDisappearAnim, emojiIdDisappearAnim)
        if (onEnd != null) {
            animSet.addListener(onEnd = { onEnd() })
        }
        animSet.start()
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.searchEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(ui.searchEditText, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /**
     * Open QR code scanner on button click.
     */
    private fun onQRButtonClick(view: View) {
        view.temporarilyDisableClick()
        requireActivity().hideKeyboard()
        hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrData = data.getStringExtra(QRScannerActivity.EXTRA_QR_DATA) ?: return
            (viewModel.deeplinkHandler.handle(qrData) as? DeepLink.Send)?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val tariWalletAddress = viewModel.getWalletAddressFromHexString(it.walletAddressHex)
                    if (tariWalletAddress != null) {
                        ui.rootView.post { ui.searchEditText.setText(tariWalletAddress.emojiId, TextView.BufferType.EDITABLE) }
                        ui.searchEditText.postDelayed({ ui.searchEditTextScrollView.smoothScrollTo(0, 0) }, Constants.UI.mediumDurationMs)
                    }
                }
            }

            (viewModel.deeplinkHandler.handle(qrData) as? DeepLink.AddBaseNode)?.let { deeplinkViewModel.executeAction(requireContext(), it) }
        }
    }

    /**
     * Dimmer clicked - hide paste-related views.
     */
    private fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            requireActivity().hideKeyboard()
            ui.searchEditText.clearFocus()
        }
    }

    /**
     * Paste banner clicked.
     */
    private fun onPasteEmojiIdButtonClicked() {
        hidePasteEmojiIdViewsOnTextChanged = false
        hidePasteEmojiIdViews(animate = true) {
            ui.searchEditText.scaleX = 0f
            ui.searchEditText.scaleY = 0f
            ui.searchEditText.setText(viewModel.clipboardTariWalletAddress.value?.emojiId, TextView.BufferType.EDITABLE)
            ui.searchEditText.setSelection(ui.searchEditText.text?.length ?: 0)
            ui.rootView.postDelayed({ animateEmojiIdPaste() }, Constants.UI.xShortDurationMs)
        }
    }

    private fun animateEmojiIdPaste() {
        // animate text size
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.searchEditText.scaleX = value
                ui.searchEditText.scaleY = value
                // searchEditText.translationX = -width * (1f - value) / 2f
            }
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(Ease.BACK_OUT)
            start()
        }
        ui.rootView.postDelayed({
            ui.searchEditTextScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        isDeletingSeparatorAtIndex =
            (if (count == 1 && after == 0 && s[start].toString() == string(R.string.emoji_id_chunk_separator)) start else null)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(editable: Editable) {
        if (hidePasteEmojiIdViewsOnTextChanged) {
            hidePasteEmojiIdViews(animate = true)
            hidePasteEmojiIdViewsOnTextChanged = false
        }
        if (textWatcherIsRunning) {
            return
        }
        textChangedProcessHandler.removeCallbacks(textChangedProcessRunnable)
        textChangedProcessHandler.postDelayed(textChangedProcessRunnable, textChangedProcessDelayMs)
    }

    private fun processTextChanged() {
        textWatcherIsRunning = true
        val editable = ui.searchEditText.editableText
        var text = editable.toString()

        ui.toolbar.clearRightIcon()
        ui.invalidEmojiIdTextView.gone()

        if (editable.toString().firstNCharactersAreEmojis(Constants.Wallet.emojiFormatterChunkSize)) {
            // if deleting a separator, first get the index of the character before that separator
            // and delete that character
            if (isDeletingSeparatorAtIndex != null) {
                val index =
                    EmojiUtil.getStartIndexOfItemEndingAtIndex(text, isDeletingSeparatorAtIndex!!)
                editable.delete(index, isDeletingSeparatorAtIndex!!)
                isDeletingSeparatorAtIndex = null
                text = editable.toString()
            }

            // delete all separators first
            val separator = string(R.string.emoji_id_chunk_separator)
            for ((offset, index) in EmojiUtil.getExistingChunkSeparatorIndices(
                text,
                separator
            ).withIndex()) {
                val target = index - (offset * separator.length)
                editable.delete(target, target + separator.length)
            }
            val textWithoutSeparators = editable.toString()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                .withIndex()) {
                val chunkSeparatorSpannable = EmojiUtil.getChunkSeparatorSpannable(
                    separator,
                    viewModel.paletteManager.getLightGray(requireContext())
                )
                val target = index + (offset * separator.length)
                editable.insert(target, chunkSeparatorSpannable)
            }
            // check if valid emoji - don't search if not
            val emojisNumber = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || emojisNumber > Constants.Wallet.emojiIdLength) {
                viewModel.selectedTariWalletAddress.value = null
                ui.invalidEmojiIdTextView.text = string(R.string.add_recipient_invalid_emoji_id)
                ui.invalidEmojiIdTextView.visible()
            } else {
                if (emojisNumber == Constants.Wallet.emojiIdLength) {
                    finishEntering(textWithoutSeparators)
                } else {
                    viewModel.selectedTariWalletAddress.value = null
                    viewModel.searchText.value = textWithoutSeparators
                }
            }
        } else if (viewModel.checkForWalletAddressHex(text)) {
            finishEntering(viewModel.selectedTariWalletAddress.value!!.emojiId)
        } else {
            viewModel.selectedTariWalletAddress.value = null
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            ui.searchEditText.letterSpacing = inputNormalLetterSpacing
            viewModel.searchText.value = editable.toString()
        }
        textWatcherIsRunning = false
    }

    private fun finishEntering(text: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.selectedTariWalletAddress.value = viewModel.getWalletAddressFromEmojiId(text)
            if (viewModel.selectedTariWalletAddress.value == null) {
                ui.invalidEmojiIdTextView.text = string(R.string.add_recipient_invalid_emoji_id)
                ui.invalidEmojiIdTextView.visible()
            } else {
                ui.invalidEmojiIdTextView.gone()
                ui.toolbar.setupRightButton(getString(R.string.contact_book_add_contact_next_button))
                activity?.hideKeyboard()
                ui.searchEditText.clearFocus()
                viewModel.searchText.value = text
            }
        }
    }
}

