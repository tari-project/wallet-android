package com.tari.android.wallet.ui.fragment.contactBook.contactSelection

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.databinding.FragmentContactsSelectionBinding
import com.tari.android.wallet.extension.launchAndRepeatOnLifecycle
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.component.clipboardController.ClipboardController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.contactSelection.ContactSelectionModel.Effect
import com.tari.android.wallet.ui.fragment.contactBook.contactSelection.ContactSelectionModel.YatState
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.ContactListAdapter
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactItemViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactlessPaymentItem
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.firstNCharactersAreEmojis
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

open class ContactSelectionFragment : CommonFragment<FragmentContactsSelectionBinding, ContactSelectionViewModel>(), TextWatcher {

    private var clipboardController: ClipboardController? = null

    private var recyclerViewAdapter = ContactListAdapter()

    /**
     * Fields related to emoji id input masking.
     */
    private var isDeletingSeparatorAtIndex: Int? = null

    private var textWatcherIsRunning = false
    private val inputNormalLetterSpacing = 0.04f
    private val inputEmojiIdLetterSpacing = 0.27f
    private val textChangedProcessDelayMs = 100L
    private val addressSeparator
        get() = string(R.string.emoji_id_chunk_separator)

    private val textChangedProcessHandler = Handler(Looper.getMainLooper())
    private var textChangedProcessRunnable = Runnable { processTextChanged() }

    private var withToolbar = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsSelectionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withToolbar = arguments?.getBoolean("withToolbar") ?: true

        val viewModel: ContactSelectionViewModel by viewModels()
        bindViewModel(viewModel)

        clipboardController = ClipboardController(listOf(ui.dimmerView), ui.clipboard, viewModel.walletAddressViewModel)

        setupUI()

        subscribeViewModal()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardController?.onDestroy()
    }

    open fun goToNext() {
        requireActivity().hideKeyboard(ui.searchEditText)
        ui.searchEditText.clearFocus()
    }

    private fun subscribeViewModal() = with(viewModel) {
        observe(contactList) { recyclerViewAdapter.update(it) }

        observe(walletAddressViewModel.discoveredWalletAddressFromClipboard) { clipboardController?.showClipboardData(it) }

        observe(selectedTariWalletAddress) { address -> address?.fullEmojiId?.let { putEmojiId(it) } }

        observe(selectedContact) { contactDto ->
            putEmojiId(contactDto.contactInfo.requireWalletAddress().fullEmojiId)
            ui.addFirstNameInput.setText((contactDto.contactInfo.firstName + " " + contactDto.contactInfo.lastName).trim())
        }

        observeOnLoad(clipboardChecker)

        viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                yatState.collect { state -> handleYatState(state) }
            }

            launch {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is Effect.ShowNotValidEmojiId -> showNotValidEmojiId()
                        is Effect.ShowNextButton -> showNextButton()
                        is Effect.GoToNext -> goToNext()
                    }
                }
            }
        }
    }

    private fun putEmojiId(emojiId: String) {
        val rawEmojiId = ui.searchEditText.text.toString().replace(string(R.string.emoji_id_chunk_separator), "")
        if (rawEmojiId == emojiId) return
        ui.searchEditText.setText(emojiId)
        ui.searchEditText.setSelectionToEnd()
    }

    private fun setupUI() {
        setupRecyclerView()
        ui.toolbar.hideRightActions()
        ui.invalidEmojiIdTextView.gone()
        OverScrollDecoratorHelper.setUpOverScroll(ui.searchEditTextScrollView)
        ui.searchEditText.inputType = InputType.TYPE_NULL
        ui.qrCodeButton.setOnClickListener { onQRButtonClick(it) }
        val args = TariToolbarActionArg(title = string(R.string.common_done)) { goToNext() }
        ui.toolbar.setRightArgs(args)
        ui.continueButton.setOnClickListener { goToNext() }
        ui.yatEyeButton.setOnClickListener { viewModel.toggleYatEye() }
        ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.searchEditText.addTextChangedListener(this@ContactSelectionFragment)

        clipboardController?.listener = object : ClipboardController.ClipboardControllerListener {

            override fun onPaste(walletAddress: TariWalletAddress) {
                ui.searchEditText.scaleX = 0f
                ui.searchEditText.scaleY = 0f
                ui.searchEditText.setText(
                    viewModel.walletAddressViewModel.discoveredWalletAddressFromClipboard.value?.fullEmojiId,
                    TextView.BufferType.EDITABLE
                )
                ui.searchEditText.setSelection(ui.searchEditText.text?.length ?: 0)
                ui.rootView.postDelayed({ animateEmojiIdPaste() }, Constants.UI.xShortDurationMs)
            }

            override fun focusOnEditText(isFocused: Boolean) {
                if (isFocused) {
                    focusEditTextAndShowKeyboard()
                } else {
                    ui.searchEditText.clearFocus()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        ui.contactsListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener { holderItem ->
            when (holderItem) {
                is ContactItemViewHolderItem -> viewModel.onContactClick(holderItem.contact)
                is ContactlessPaymentItem -> viewModel.onContactlessPaymentClick()
            }
        })
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
    }

    private fun showNotValidEmojiId() {
        ui.invalidEmojiIdTextView.text = string(R.string.add_recipient_invalid_emoji_id)
        ui.invalidEmojiIdTextView.visible()
    }

    private fun showNextButton() {
        ui.invalidEmojiIdTextView.gone()
        ui.toolbar.setRightArgs(TariToolbarActionArg(title = string(R.string.contact_book_add_contact_next_button)) { goToNext() })
        ui.toolbar.showRightActions()
        if (!withToolbar) {
            ui.continueButton.visible()
        }
        activity?.hideKeyboard()
        ui.searchEditText.clearFocus()
    }

    private fun handleYatState(yatState: YatState) {
        ui.yatEyeButton.setVisible(yatState.showYatIcons)
        ui.yatIcon.setVisible(yatState.showYatIcons)
        ui.searchEditText.hint = string(
            if (DebugConfig.isYatEnabled) R.string.contact_book_add_contact_placeholder
            else R.string.contact_book_add_contact_placeholder_no_yat
        )

        if (yatState.yatUser != null) {
            if (yatState.eyeOpened) {
                ui.searchEditText.removeTextChangedListener(this)
                ui.yatEyeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vector_closed_eye))
                ui.searchEditText.setText(yatState.yatUser.walletAddress.fullBase58)
            } else {
                ui.searchEditText.removeTextChangedListener(this)
                ui.searchEditText.setText(yatState.yatUser.yat)
                ui.searchEditText.setSelectionToEnd()
                ui.searchEditText.addTextChangedListener(this)
                ui.yatEyeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vector_opened_eye))
            }
        }
    }

    open fun startQRCodeActivity() {
        QrScannerActivity.startScanner(this, QrScannerSource.AddContact)
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
        clipboardController?.hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(qrDeepLink)
        }
    }

    private fun animateEmojiIdPaste() {
        // animate text size
        animations += ValueAnimator.ofFloat(0f, 1f).apply {
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
            if (count == 1 && after == 0 && s[start].toString() == addressSeparator) start else null
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(editable: Editable) {
        clipboardController?.let { clipboardController ->
            if (clipboardController.hidePasteEmojiIdViewsOnTextChanged) {
                clipboardController.hidePasteEmojiIdViews(animate = true)
                clipboardController.hidePasteEmojiIdViewsOnTextChanged = false
            }
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

        ui.toolbar.hideRightActions()
        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()

        if (editable.toString().firstNCharactersAreEmojis(Constants.Wallet.EMOJI_FORMATTER_CHUNK_SIZE)) {
            // if deleting a separator, first get the index of the character before that separator
            // and delete that character
            if (isDeletingSeparatorAtIndex != null) {
                val index = EmojiUtil.getStartIndexOfItemEndingAtIndex(text, isDeletingSeparatorAtIndex!!)
                editable.delete(index, isDeletingSeparatorAtIndex!!)
                isDeletingSeparatorAtIndex = null
                text = editable.toString()
            }

            // delete all separators first
            for ((offset, index) in EmojiUtil.getExistingChunkSeparatorIndices(text, addressSeparator).withIndex()) {
                val target = index - (offset * addressSeparator.length)
                editable.delete(target, target + addressSeparator.length)
            }
            val textWithoutSeparators = editable.toString()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators).withIndex()) {
                val chunkSeparatorSpannable = EmojiUtil.getChunkSeparatorSpannable(
                    separator = addressSeparator,
                    color = PaletteManager.getLightGray(requireContext()),
                )
                val target = index + (offset * addressSeparator.length)
                editable.insert(target, chunkSeparatorSpannable)
            }
            // check if valid emoji - don't search if not
            if (textWithoutSeparators.containsNonEmoji()) {
                viewModel.deselectTariWalletAddress()
                showNotValidEmojiId()
            } else {
                viewModel.addressEntered(textWithoutSeparators)
            }
        } else if (viewModel.deeplinkManager.parseDeepLink(text) != null) {
            viewModel.parseDeeplink(requireActivity(), text)
        } else {
            val walletAddress = TariWalletAddress.fromBase58OrNull(text)
            if (walletAddress != null) {
                viewModel.addressEntered(walletAddress.fullEmojiId)
            } else {
                viewModel.deselectTariWalletAddress()
                ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                ui.searchEditText.letterSpacing = inputNormalLetterSpacing
                viewModel.addressEntered(editable.toString())
            }
        }
        textWatcherIsRunning = false
    }
}
