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
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindColor
import butterknife.BindDimen
import butterknife.BindString
import butterknife.ButterKnife
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.application.DeepLink.Type.EMOJI_ID
import com.tari.android.wallet.application.DeepLink.Type.PUBLIC_KEY_HEX
import com.tari.android.wallet.databinding.FragmentAddRecipientBinding
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.activity.qr.EXTRA_QR_DATA
import com.tari.android.wallet.ui.activity.qr.QRScannerActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.send.adapter.RecipientListAdapter
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.*
import com.tari.android.wallet.util.Constants.Wallet.emojiFormatterChunkSize
import com.tari.android.wallet.util.Constants.Wallet.emojiIdLength
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.min

/**
 * Fragment that manages the adding of a recipient to an outgoing transaction.
 *
 * @author The Tari Development Team
 */

class AddRecipientFragment(private val walletService: TariWalletService) : Fragment(),
    RecipientListAdapter.Listener,
    RecyclerView.OnItemTouchListener,
    TextWatcher {
    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator)
    lateinit var emojiIdChunkSeparator: String

    @BindString(R.string.add_recipient_invalid_emoji_id)
    lateinit var invalidEmojiIdMessage: String

    @BindString(R.string.add_recipient_own_emoji_id)
    lateinit var ownEmojiIdMessage: String

    @BindDimen(R.dimen.add_recipient_contact_list_item_height)
    @JvmField
    var listItemHeight = 0

    @BindDimen(R.dimen.add_recipient_clipboard_emoji_id_container_height)
    @JvmField
    var emojiIdContainerHeight = 0

    @BindDimen(R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin)
    @JvmField
    var pasteEmojiIdButtonVisibleTopMargin = 0

    @BindColor(R.color.add_recipient_prog_bar)
    @JvmField
    var progressBarColor = 0

    @BindColor(R.color.black)
    @JvmField
    var blackColor = 0

    @BindColor(R.color.light_gray)
    @JvmField
    var lightGrayColor = 0

    private lateinit var regularFont: Typeface
    private lateinit var emojiFont: Typeface

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var clipboardManager: ClipboardManager

    /**
     * List, adapter & layout manager.
     */
    private lateinit var recyclerViewAdapter: RecipientListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private var scrollListener = ScrollListener(this)

    private lateinit var recentTxUsers: List<User>
    private lateinit var contacts: List<Contact>
    private lateinit var allPastTxUsers: MutableList<User>

    private val recentTxContactsLimit = 6
    private var wr = WeakReference(this)
    private var recipientsListedForTheFirstTime = true

    private lateinit var listenerWR: WeakReference<Listener>

    /**
     * Will be non-null if there's a valid emoji id in the clipboard
     */
    private var emojiIdPublicKey: PublicKey? = null

    /**
     * Fields related to emoji id input masking.
     */
    private var isDeletingSeparatorAtIndex: Int? = null
    private var textWatcherIsRunning = false
    private var inputNormalLetterSpacing = 0.04f
    private var inputEmojiIdLetterSpacing = 0.27f

    private val textChangedProcessDelayMs = 100L
    private val textChangedProcessHandler = Handler(Looper.getMainLooper())
    private var textChangedProcessRunnable = Runnable {
        processTextChanged()
    }

    private var hidePasteEmojiIdViewsOnTextChanged = false

    companion object {

        private const val REGULAR_FONT_NAME = "AVENIR_LT_STD_ROMAN"

        // TODO [CRASH]
        fun newInstance(walletService: TariWalletService): AddRecipientFragment {
            return AddRecipientFragment(walletService)
        }

    }

    private var _ui: FragmentAddRecipientBinding? = null
    private val ui get() = _ui!!

    /**
     * Paste-emoji-id-related views.
     */
    private val dimmerViews
        get() = arrayOf(
            ui.topDimmerView,
            ui.middleDimmerView,
            ui.bottomDimmerView
        )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentAddRecipientBinding.inflate(inflater, container, false).also { _ui = it }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AddRecipientFragmentVisitor.visit(this, view)
        // initialize recycler view
        setupUi()
        // fetch data
        Thread {
            fetchAllData {
                ui.rootView.post {
                    displayInitialList()
                    ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
                    ui.searchEditText.addTextChangedListener(this)
                }
                checkClipboardForValidEmojiId()
            }
        }.start()

        TrackHelper.track()
            .screen("/home/send_tari/add_recipient")
            .title("Send Tari - Add Recipient")
            .with(tracker)
    }

    private fun setupUi() {
        regularFont = CustomFont.fromString(REGULAR_FONT_NAME).asTypeface(requireContext())
        emojiFont = ResourcesCompat.getFont(requireContext(), R.font.noto_color_emoji)!!
        recyclerViewLayoutManager = LinearLayoutManager(activity)
        ui.contactsListRecyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter = RecipientListAdapter(this)
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
        ui.contactsListRecyclerView.addOnScrollListener(scrollListener)
        ui.contactsListRecyclerView.addOnItemTouchListener(this)
        ui.scrollDepthGradientView.alpha = 0f
        UiUtil.setProgressBarColor(ui.progressBar, progressBarColor)
        ui.progressBar.visible()
        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()
        hidePasteEmojiIdViews(animate = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.emojiIdScrollView)
        OverScrollDecoratorHelper.setUpOverScroll(ui.searchEditTextScrollView)
        ui.searchEditText.inputType = InputType.TYPE_NULL
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.qrCodeButton.setOnClickListener { onQRButtonClick() }
        ui.continueButton.setOnClickListener { onContinueButtonClicked(it) }
        dimmerViews.forEach { it.setOnClickListener { onEmojiIdDimmerClicked() } }
        ui.pasteEmojiIdButton.setOnClickListener { onPasteEmojiIdButtonClicked() }
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (ui.searchEditText.text.toString().isNotEmpty()) {
            ui.searchEditText.setText("")
            ui.searchEditText.isEnabled = true
            ui.continueButton.gone()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    private fun startQRCodeActivity() {
        val intent = Intent(activity, QRScannerActivity::class.java)
        startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
        activity?.overridePendingTransition(R.anim.slide_up, 0)
    }

    /**
     * Checks whether a valid deep link or an emoji id is in the clipboard data.
     */
    private fun checkClipboardForValidEmojiId() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            ?: return
        val deepLink = DeepLink.from(clipboardString)
        if (deepLink != null) {
            // there is a deep link in the clipboard
            emojiIdPublicKey = when (deepLink.type) {
                EMOJI_ID -> walletService.getPublicKeyFromEmojiId(deepLink.type.value)
                PUBLIC_KEY_HEX -> walletService.getPublicKeyFromHexString(deepLink.type.value)
            }
        } else if (clipboardString.isPossiblyEmojiId()) { // check if clipboard data is emoji id
            // there is an emoji id in the clipboard
            emojiIdPublicKey = walletService.getPublicKeyFromEmojiId(clipboardString)
        } else { // might be a chunked emoji-id
            val cleanEmojiId = clipboardString.trim().extractEmojis()
            if (cleanEmojiId.isPossiblyEmojiId()) {
                // there is a chunked emoji id in the clipboard
                emojiIdPublicKey = walletService.getPublicKeyFromEmojiId(cleanEmojiId)
            }
        }
        emojiIdPublicKey?.let {
            if (it.emojiId != sharedPrefsWrapper.emojiId!!) {
                ui.contactsListRecyclerView.post {
                    wr.get()?.hidePasteEmojiIdViewsOnTextChanged = true
                    wr.get()?.showPasteEmojiIdViews(it)
                    wr.get()?.focusEditTextAndShowKeyboard()
                }
            }

        }
    }

    /**
     * Displays paste-emoji-id-related views.
     */
    private fun showPasteEmojiIdViews(publicKey: PublicKey) {
        ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            publicKey.emojiId,
            emojiIdChunkSeparator,
            blackColor,
            lightGrayColor
        )
        UiUtil.setBottomMargin(
            ui.emojiIdContainerView,
            -emojiIdContainerHeight
        )
        ui.emojiIdContainerView.visible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }

        // animate
        val emojiIdAppearAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAppearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val animValue = valueAnimator.animatedValue as Float
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = animValue * 0.6f
            }
            UiUtil.setBottomMargin(
                ui.emojiIdContainerView,
                (-emojiIdContainerHeight * (1f - animValue)).toInt()
            )
        }
        emojiIdAppearAnim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        emojiIdAppearAnim.duration = Constants.UI.mediumDurationMs

        // animate and show paste emoji id button
        UiUtil.setTopMargin(ui.pasteEmojiIdContainerView, 0)
        val pasteButtonAppearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonAppearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setTopMargin(
                ui.pasteEmojiIdContainerView,
                (pasteEmojiIdButtonVisibleTopMargin * value).toInt()
            )
            ui.pasteEmojiIdContainerView.alpha = value
        }
        pasteButtonAppearAnim.addListener(onStart = { ui.pasteEmojiIdContainerView.visible() })
        pasteButtonAppearAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        pasteButtonAppearAnim.duration = Constants.UI.shortDurationMs

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
        animSet.startDelay = Constants.UI.xShortDurationMs
        animSet.start()
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
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setTopMargin(
                ui.pasteEmojiIdContainerView,
                (pasteEmojiIdButtonVisibleTopMargin * (1 - value)).toInt()
            )
            ui.pasteEmojiIdContainerView.alpha = (1 - value)
        }
        pasteButtonDisappearAnim.addListener(onEnd = { ui.pasteEmojiIdContainerView.gone() })
        pasteButtonDisappearAnim.duration = Constants.UI.shortDurationMs
        // animate and hide emoji id & dimmers
        val emojiIdDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = 0.6f * (1 - value)
            }
            UiUtil.setBottomMargin(
                ui.emojiIdContainerView,
                (-emojiIdContainerHeight * value).toInt()
            )
        }
        emojiIdDisappearAnim.addListener(onEnd = {
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
        })
        emojiIdDisappearAnim.duration = Constants.UI.shortDurationMs

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(pasteButtonDisappearAnim, emojiIdDisappearAnim)
        if (onEnd != null) {
            animSet.addListener(onEnd = { onEnd() })
        }
        animSet.start()
    }

    /**
     * Called only once in onCreate.
     */
    private fun fetchAllData(onComplete: () -> Unit) {
        val error = WalletError()
        contacts = walletService.getContacts(error)
        recentTxUsers = walletService.getRecentTxUsers(recentTxContactsLimit, error)
        val completedTxs = walletService.getCompletedTxs(error)
        val pendingInboundTxs = walletService.getPendingInboundTxs(error)
        val pendingOutboundTxs = walletService.getPendingOutboundTxs(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        val allTxs = ArrayList<Tx>()
        allTxs.addAll(completedTxs)
        allTxs.addAll(pendingInboundTxs)
        allTxs.addAll(pendingOutboundTxs)
        allPastTxUsers = mutableListOf()
        // TODO happens-before relationship is not guaranteed
        allTxs.forEach { tx ->
            if (!allPastTxUsers.contains(tx.user)) {
                allPastTxUsers.add(tx.user)
            }
        }
        onComplete()
    }

    /**
     * Displays non-search-result list.
     */
    private fun displayInitialList() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displayList(recentTxUsers, contacts)

        if (recipientsListedForTheFirstTime) { // show keyboard
            recipientsListedForTheFirstTime = false
            focusEditTextAndShowKeyboard()
        }
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.searchEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    private fun onSearchTextChanged(query: String) {
        ui.contactsListRecyclerView.invisible()
        scrollListener.reset()
        ui.scrollDepthGradientView.alpha = 0f
        ui.progressBar.visible()
        if (query.isEmpty()) {
            displayInitialList()
        } else {
            searchRecipients(query)
        }
    }

    private fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        ui.rootView.postDelayed({
            mActivity.onBackPressed()
        }, 200L)
    }

    /**
     * Makes a search by the input.
     */
    private fun searchRecipients(query: String) {
        val users = ArrayList<User>()
        // get all contacts and filter by alias and emoji id
        val filteredContacts = contacts.filter {
            it.alias.contains(query, ignoreCase = true) || it.publicKey.emojiId.contains(query)
        }
        users.addAll(filteredContacts)
        allPastTxUsers.forEach { txUser ->
            if (txUser.publicKey.emojiId.contains(query)) {
                if (!users.contains(txUser)) {
                    users.add(txUser)
                }
            }
        }
        displaySearchResult(users)
    }

    private fun clearSearchResult() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displaySearchResult(ArrayList())
    }

    private fun displaySearchResult(users: List<User>) {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displaySearchResult(users)
    }

    /**
     * Recipient selected from the list.
     */
    override fun onRecipientSelected(recipient: User) {
        // go to amount fragment
        listenerWR.get()?.continueToAmount(this, recipient)
    }

    /**
     * Open QR code scanner on button click.
     */
    private fun onQRButtonClick() {
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed({
                wr.get()?.startQRCodeActivity()
            }, Constants.UI.keyboardHideWaitMs)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER
            && resultCode == Activity.RESULT_OK
            && data != null
        ) {
            val qrData = data.getStringExtra(EXTRA_QR_DATA) ?: return
            val deepLink = DeepLink.from(qrData) ?: return
            when (deepLink.type) {
                EMOJI_ID -> {
                    ui.searchEditText.setText(
                        deepLink.type.value,
                        TextView.BufferType.EDITABLE
                    )
                    ui.searchEditText.postDelayed({
                        ui.searchEditTextScrollView.smoothScrollTo(0, 0)
                    }, Constants.UI.mediumDurationMs)
                }
                PUBLIC_KEY_HEX -> {
                    AsyncTask.execute {
                        val publicKeyHex = deepLink.type.value
                        val publicKey = walletService.getPublicKeyFromHexString(publicKeyHex)
                        if (publicKey != null) {
                            ui.searchEditText.post {
                                wr.get()?.ui?.searchEditText?.setText(
                                    publicKey.emojiId,
                                    TextView.BufferType.EDITABLE
                                )
                            }
                            ui.searchEditText.postDelayed({
                                wr.get()?.ui?.searchEditTextScrollView?.smoothScrollTo(0, 0)
                            }, Constants.UI.mediumDurationMs)
                        }
                    }
                }
            }
        }
    }

    private fun onContinueButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        AsyncTask.execute {
            val error = WalletError()
            val contacts = walletService.getContacts(error)
            val recipientContact: Contact? = when (error.code) {
                WalletErrorCode.NO_ERROR -> contacts.firstOrNull { it.publicKey == emojiIdPublicKey }
                else -> null
            }
            ui.rootView.post {
                listenerWR.get()?.continueToAmount(
                    this,
                    recipientContact ?: User(emojiIdPublicKey!!)
                )
            }
        }
    }

    /**
     * Dimmer clicked - hide paste-related views.
     */
    private fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            val mActivity = activity
            if (mActivity != null) {
                UiUtil.hideKeyboard(mActivity)
                ui.searchEditText.clearFocus()
            }
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
            ui.searchEditText.setText(
                emojiIdPublicKey!!.emojiId,
                TextView.BufferType.EDITABLE
            )
            ui.searchEditText.setSelection(ui.searchEditText.text?.length ?: 0)

            ui.rootView.postDelayed({
                animateEmojiIdPaste()
            }, Constants.UI.xShortDurationMs)
        }
    }

    private fun animateEmojiIdPaste() {
        // animate text size
        val textAnim = ValueAnimator.ofFloat(0f, 1f)
        textAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.searchEditText.scaleX = value
            ui.searchEditText.scaleY = value
            // searchEditText.translationX = -width * (1f - value) / 2f
        }
        textAnim.duration = Constants.UI.shortDurationMs
        textAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        textAnim.start()
        ui.rootView.postDelayed({
            ui.searchEditTextScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs)
    }

    // region recycler view item touch listener

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // no-op
    }

    /**
     * Hide keyboard on list touch.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val mActivity = activity ?: return false
        UiUtil.hideKeyboard(mActivity)
        ui.searchEditText.clearFocus()
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // no-op
    }

    // endregion

    // region emoji id input masking

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        isDeletingSeparatorAtIndex =
            if (count == 1 && after == 0 && s[start].toString() == emojiIdChunkSeparator) {
                start
            } else {
                null
            }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // no-op
    }

    override fun afterTextChanged(editable: Editable) {
        if (hidePasteEmojiIdViewsOnTextChanged) {
            hidePasteEmojiIdViews(animate = true)
            hidePasteEmojiIdViewsOnTextChanged = false
        }
        if (textWatcherIsRunning) {
            return
        }
        textChangedProcessHandler.removeCallbacks(
            textChangedProcessRunnable
        )
        textChangedProcessHandler.postDelayed(
            textChangedProcessRunnable,
            textChangedProcessDelayMs
        )
    }

    private fun processTextChanged() {
        textWatcherIsRunning = true
        val editable = ui.searchEditText.editableText
        var text = editable.toString()
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
        for ((offset, index) in EmojiUtil.getExistingChunkSeparatorIndices(
            text,
            emojiIdChunkSeparator
        ).withIndex()) {
            val target = index - (offset * emojiIdChunkSeparator.length)
            editable.delete(target, target + emojiIdChunkSeparator.length)
        }

        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()

        val textWithoutSeparators = editable.toString()
        if (textWithoutSeparators.firstNCharactersAreEmojis(emojiFormatterChunkSize)) {
            ui.searchEditText.typeface = emojiFont
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                .withIndex()) {
                val chunkSeparatorSpannable = EmojiUtil.getChunkSeparatorSpannable(
                    emojiIdChunkSeparator,
                    lightGrayColor
                )
                val target = index + (offset * emojiIdChunkSeparator.length)
                editable.insert(target, chunkSeparatorSpannable)
            }
            // check if valid emoji - don't search if not
            val numberofEmojis = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || numberofEmojis > emojiIdLength) {
                emojiIdPublicKey = null
                // invalid emoji-id : clear list and display error
                ui.invalidEmojiIdTextView.text = invalidEmojiIdMessage
                ui.invalidEmojiIdTextView.visible()
                ui.qrCodeButton.visible()
                clearSearchResult()
            } else {
                if (numberofEmojis == emojiIdLength) {
                    if (textWithoutSeparators == sharedPrefsWrapper.emojiId!!) {
                        emojiIdPublicKey = null
                        ui.invalidEmojiIdTextView.text = ownEmojiIdMessage
                        ui.invalidEmojiIdTextView.visible()
                        ui.qrCodeButton.visible()
                        clearSearchResult()
                    } else {
                        ui.qrCodeButton.gone()
                        // valid emoji id length - clear list, no search, display continue button
                        AsyncTask.execute {
                            emojiIdPublicKey =
                                walletService.getPublicKeyFromEmojiId(textWithoutSeparators)
                            ui.rootView.post {
                                if (emojiIdPublicKey == null) {
                                    ui.invalidEmojiIdTextView.text = invalidEmojiIdMessage
                                    ui.invalidEmojiIdTextView.visible()
                                    clearSearchResult()
                                } else {
                                    ui.invalidEmojiIdTextView.gone()
                                    ui.continueButton.visible()
                                    val mActivity = activity
                                    if (mActivity != null) {
                                        UiUtil.hideKeyboard(mActivity)
                                        ui.searchEditText.clearFocus()
                                    }
                                    onSearchTextChanged(textWithoutSeparators)
                                    ui.searchEditText.isEnabled = false
                                }
                            }
                        }
                    }
                } else {
                    emojiIdPublicKey = null
                    ui.qrCodeButton.visible()
                    onSearchTextChanged(textWithoutSeparators)
                }
            }
        } else {
            ui.searchEditText.typeface = regularFont
            emojiIdPublicKey = null
            ui.qrCodeButton.visible()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            ui.searchEditText.letterSpacing = inputNormalLetterSpacing
            onSearchTextChanged(textWithoutSeparators)
        }
        textWatcherIsRunning = false
    }

    // endregion

    // region scroll depth gradient view controls

    private fun onRecyclerViewScrolled(totalDeltaY: Int) {
        ui.scrollDepthGradientView.alpha = min(
            Constants.UI.scrollDepthShadowViewMaxOpacity,
            totalDeltaY / listItemHeight.toFloat()
        )
    }

    class ScrollListener(fragment: AddRecipientFragment) : RecyclerView.OnScrollListener() {

        private val fragmentWR = WeakReference(fragment)
        private var totalDeltaY = 0

        fun reset() {
            totalDeltaY = 0
        }

        override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
            super.onScrolled(recyclerView, dX, dY)
            totalDeltaY += dY
            fragmentWR.get()?.onRecyclerViewScrolled(totalDeltaY)
        }

    }

    // end region

    // region listener interface

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        /**
         * Send to a user from the list.
         */
        fun continueToAmount(sourceFragment: AddRecipientFragment, user: User)

    }

    // endregion

    private object AddRecipientFragmentVisitor {
        internal fun visit(fragment: AddRecipientFragment, view: View) {
            fragment.requireActivity().appComponent.inject(fragment)
            ButterKnife.bind(fragment, view)
        }
    }

}
