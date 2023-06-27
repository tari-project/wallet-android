package com.tari.android.wallet.ui.fragment.contact_book.contactSelection

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.databinding.FragmentContactsSelectionBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.component.clipboardController.ClipboardController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.postDelayed
import com.tari.android.wallet.ui.extension.setSelectionToEnd
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.ContactListAdapter
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactlessPaymentItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ShareViewModel
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

    private lateinit var clipboardController: ClipboardController

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

    private var yatEyeState = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsSelectionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactSelectionViewModel by viewModels()
        bindViewModel(viewModel)
        subscribeVM(deeplinkViewModel)

        clipboardController = ClipboardController(listOf(ui.dimmerView), ui.clipboard, viewModel.walletAddressViewModel)

        setupUI()

        subscribeViewModal()
    }

    open fun goToNext() {
        requireActivity().hideKeyboard(ui.searchEditText)
        ui.searchEditText.clearFocus()
    }

    private fun subscribeViewModal() = with(viewModel) {
        observe(list) { recyclerViewAdapter.update(it) }

        observe(walletAddressViewModel.discoveredWalletAddressFromClipboard) { clipboardController.showClipboardData(it) }

        observe(selectedTariWalletAddress) { putEmojiId(it.emojiId) }

        observe(selectedUser) { putEmojiId(it.contact.extractWalletAddress().emojiId) }

        observe(foundYatUser) { showYatUser(if (it.isPresent) it.get() else null) }

        observeOnLoad(clipboardChecker)
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
        ui.yatEyeButton.setOnClickListener { toggleYatEye() }
        ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.searchEditText.addTextChangedListener(this@ContactSelectionFragment)

        clipboardController.listener = object : ClipboardController.ClipboardControllerListener {

            override fun onPaste(walletAddress: TariWalletAddress) {
                ui.searchEditText.scaleX = 0f
                ui.searchEditText.scaleY = 0f
                ui.searchEditText.setText(
                    viewModel.walletAddressViewModel.discoveredWalletAddressFromClipboard.value?.emojiId,
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
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener {
            onItemClick(it as? ContactItem)
            (it as? ContactlessPaymentItem)?.let { onContactlessPaymentClick() }
        })
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
    }

    private fun onContactlessPaymentClick() {
        ShareViewModel.currentInstant?.doContactlessPayment()
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

    private fun showYatUser(yatUser: YatDto?) {
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
            ui.toolbar.hideRightActions()
        }
    }

    private fun startQRCodeActivity() {
        val intent = Intent(activity, QRScannerActivity::class.java)
        startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
        activity?.overridePendingTransition(R.anim.slide_up, 0)
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
        clipboardController.hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrData = data.getStringExtra(QRScannerActivity.EXTRA_QR_DATA) ?: return
            deeplinkViewModel.tryToHandle(requireContext(), qrData)
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
        if (clipboardController.hidePasteEmojiIdViewsOnTextChanged) {
            clipboardController.hidePasteEmojiIdViews(animate = true)
            clipboardController.hidePasteEmojiIdViewsOnTextChanged = false
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
        } else if (viewModel.deeplinkHandler.handle(text) != null) {
            val deeplink = viewModel.deeplinkHandler.handle(text)!!
            deeplinkViewModel.execute(requireContext(), deeplink)
            viewModel.selectedTariWalletAddress.value = null
        } else if (viewModel.walletAddressViewModel.checkForWalletAddressHex(text)) {
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
            viewModel.selectedTariWalletAddress.value = viewModel.walletAddressViewModel.getWalletAddressFromEmojiId(text)
            if (viewModel.selectedTariWalletAddress.value == null) {
                ui.invalidEmojiIdTextView.text = string(R.string.add_recipient_invalid_emoji_id)
                ui.invalidEmojiIdTextView.visible()
            } else {
                ui.invalidEmojiIdTextView.gone()
                ui.toolbar.setRightArgs(TariToolbarActionArg(title = string(R.string.contact_book_add_contact_next_button)) { goToNext() })
                ui.toolbar.showRightActions()
                activity?.hideKeyboard()
                ui.searchEditText.clearFocus()
                viewModel.searchText.value = text
            }
        }
    }
}

