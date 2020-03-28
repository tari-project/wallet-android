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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.application.DeepLink.Type.EMOJI_ID
import com.tari.android.wallet.application.DeepLink.Type.PUBLIC_KEY_HEX
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.activity.qr.EXTRA_QR_DATA
import com.tari.android.wallet.ui.activity.qr.QRScannerActivity
import com.tari.android.wallet.ui.fragment.BaseFragment
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

class AddRecipientFragment(private val walletService: TariWalletService) : BaseFragment(),
    RecipientListAdapter.Listener,
    RecyclerView.OnItemTouchListener,
    TextWatcher {

    @BindView(R.id.add_recipient_vw_root)
    lateinit var rootView: View
    @BindView(R.id.send_tari_add_recipient_rv_contact_list)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.add_recipient_rv_scroll_depth_gradient)
    lateinit var scrollDepthView: View
    @BindView(R.id.add_recipient_prog_bar)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.add_recipient_scroll_search_edit_text)
    lateinit var searchEditTextScrollView: HorizontalScrollView
    @BindView(R.id.add_recipient_edit_search)
    lateinit var searchEditText: EditText
    @BindView(R.id.add_recipient_txt_title)
    lateinit var titleTextView: TextView
    @BindView(R.id.add_recipient_btn_continue)
    lateinit var continueButton: Button
    @BindView(R.id.add_recipient_txt_invalid_emoji_id)
    lateinit var invalidEmojiIdTextView: TextView
    @BindView(R.id.add_recipient_btn_qr_code)
    lateinit var qrCodeButton: ImageButton
    /**
     * Paste-emoji-id-related views.
     */
    @BindViews(
        R.id.add_recipinet_vw_top_dimmer,
        R.id.add_recipinet_vw_middle_dimmer,
        R.id.add_recipinet_vw_bottom_dimmer
    )
    lateinit var dimmerViews: List<@JvmSuppressWildcards View>
    @BindView(R.id.add_recipient_vw_paste_emoji_id_container)
    lateinit var pasteEmojiIdContainerView: View
    @BindView(R.id.add_recipient_vw_clipboard_emoji_id_container)
    lateinit var emojiIdContainerView: View
    @BindView(R.id.add_recipient_scroll_clipboard_emoji_id)
    lateinit var emojiIdScrollView: HorizontalScrollView
    @BindView(R.id.add_recipient_txt_clipboard_emoji_id)
    lateinit var emojiIdTextView: TextView

    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator_char)
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

    override val contentViewId: Int = R.layout.fragment_add_recipient

    companion object {

        fun newInstance(walletService: TariWalletService): AddRecipientFragment {
            return AddRecipientFragment(walletService)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize recycler view
        recyclerViewLayoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter = RecipientListAdapter(this)
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.addOnItemTouchListener(this)

        scrollDepthView.alpha = 0f
        UiUtil.setProgressBarColor(progressBar, progressBarColor)
        progressBar.visibility = View.VISIBLE

        continueButton.visibility = View.GONE
        invalidEmojiIdTextView.visibility = View.GONE
        searchEditText.addTextChangedListener(this)

        hidePasteEmojiIdViews(animate = false)

        OverScrollDecoratorHelper.setUpOverScroll(emojiIdScrollView)
        OverScrollDecoratorHelper.setUpOverScroll(searchEditTextScrollView)

        AsyncTask.execute {
            wr.get()?.fetchRecentTxUsers()
        }

        AsyncTask.execute {
            wr.get()?.checkClipboardForValidEmojiId()
        }

        TrackHelper.track()
            .screen("/home/send_tari/add_recipient")
            .title("Send Tari - Add Recipient")
            .with(tracker)
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (searchEditText.text.toString().isNotEmpty()) {
            searchEditText.setText("")
            searchEditText.isEnabled = true
            continueButton.visibility = View.GONE
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
            val cleanEmojiId = EmojiUtil.removeChunkSeparatorsFromEmojiId(
                clipboardString.trim(),
                emojiIdChunkSeparator
            )
            if (cleanEmojiId.isPossiblyEmojiId()) {
                // there is a chunked emoji id in the clipboard
                emojiIdPublicKey = walletService.getPublicKeyFromEmojiId(cleanEmojiId)
            }
        }
        emojiIdPublicKey?.let {
            if (it.emojiId != sharedPrefsWrapper.emojiId!!) {
                recyclerView.post {
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
        emojiIdTextView.text = EmojiUtil.getChunkedEmojiId(
            publicKey.emojiId,
            emojiIdChunkSeparator
        )
        UiUtil.setBottomMargin(
            emojiIdContainerView,
            -emojiIdContainerHeight
        )
        emojiIdContainerView.visibility = View.VISIBLE
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visibility = View.VISIBLE
        }

        // animate
        val emojiIdAppearAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAppearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val animValue = valueAnimator.animatedValue as Float
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = animValue * 0.6f
            }
            UiUtil.setBottomMargin(
                emojiIdContainerView,
                (-emojiIdContainerHeight * (1f - animValue)).toInt()
            )
        }
        emojiIdAppearAnim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        emojiIdAppearAnim.duration = Constants.UI.mediumDurationMs

        // animate and show paste emoji id button
        UiUtil.setTopMargin(pasteEmojiIdContainerView, 0)
        val pasteButtonAppearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonAppearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setTopMargin(
                pasteEmojiIdContainerView,
                (pasteEmojiIdButtonVisibleTopMargin * value).toInt()
            )
            pasteEmojiIdContainerView.alpha = value
        }
        pasteButtonAppearAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                pasteEmojiIdContainerView.visibility = View.VISIBLE
            }
        })
        pasteButtonAppearAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        pasteButtonAppearAnim.duration = Constants.UI.shortDurationMs

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
        animSet.startDelay = Constants.UI.shortDurationMs
        animSet.start()
    }

    /**
     * Hides paste-emoji-id-related views.
     */
    private fun hidePasteEmojiIdViews(animate: Boolean, onEnd: (() -> (Unit))? = null) {
        if (!animate) {
            pasteEmojiIdContainerView.visibility = View.GONE
            emojiIdContainerView.visibility = View.GONE
            dimmerViews.forEach {
                it.visibility = View.GONE
            }
            onEnd?.let { it() }
            return
        }
        // animate and hide paste emoji id button
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setTopMargin(
                pasteEmojiIdContainerView,
                (pasteEmojiIdButtonVisibleTopMargin * (1 - value)).toInt()
            )
            pasteEmojiIdContainerView.alpha = (1 - value)
        }
        pasteButtonDisappearAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                pasteEmojiIdContainerView.visibility = View.GONE
            }
        })
        pasteButtonDisappearAnim.duration = Constants.UI.shortDurationMs
        // animate and hide emoji id & dimmers
        val emojiIdDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = 0.6f * (1 - value)
            }
            UiUtil.setBottomMargin(
                emojiIdContainerView,
                (-emojiIdContainerHeight * value).toInt()
            )
        }
        emojiIdDisappearAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                emojiIdContainerView.visibility = View.GONE
                dimmerViews.forEach {
                    it.visibility = View.GONE
                }
            }
        })
        emojiIdDisappearAnim.duration = Constants.UI.shortDurationMs

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(pasteButtonDisappearAnim, emojiIdDisappearAnim)
        if (onEnd != null) {
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    onEnd()
                }
            })
        }
        animSet.start()
    }

    /**
     * Fetches users with whom this wallet had recent transactions.
     */
    private fun fetchRecentTxUsers() {
        val error = WalletError()
        val users = walletService.getRecentTxUsers(recentTxContactsLimit, error)
        val contacts = walletService.getContacts(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        recyclerView.post {
            wr.get()?.displayList(users, contacts)
        }
    }

    /**
     * Displays non-search-result list.
     */
    private fun displayList(users: List<User>, contacts: List<Contact>) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerViewAdapter.displayList(users, contacts)
        if (recipientsListedForTheFirstTime) { // show keyboard
            recipientsListedForTheFirstTime = false
            focusEditTextAndShowKeyboard()
        }
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        searchEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    private fun onSearchTextChanged(query: String) {
        recyclerView.visibility = View.INVISIBLE
        scrollListener.reset()
        scrollDepthView.alpha = 0f
        progressBar.visibility = View.VISIBLE
        if (query.isEmpty()) {
            AsyncTask.execute {
                wr.get()?.fetchRecentTxUsers()
            }
        } else {
            AsyncTask.execute {
                wr.get()?.searchRecipients(query)
            }
        }
    }

    @OnClick(R.id.add_recipient_btn_back)
    fun onBackButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        rootView.postDelayed({
            mActivity.onBackPressed()
        }, 200L)
    }

    /**
     * Makes a search by the input.
     */
    private fun searchRecipients(query: String) {
        val error = WalletError()
        val contacts = walletService.getContacts(error)
        val completedTxs = walletService.getCompletedTxs(error)
        val pendingInboundTxs = walletService.getPendingInboundTxs(error)
        val pendingOutboundTxs = walletService.getPendingOutboundTxs(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        val users = ArrayList<User>()
        // get all contacts and filter by alias and emoji id
        val filteredContacts = contacts.filter {
            it.alias.contains(query, ignoreCase = true) || it.publicKey.emojiId.contains(query)
        }
        users.addAll(filteredContacts)
        val allTxs = ArrayList<Tx>()
        allTxs.addAll(completedTxs)
        allTxs.addAll(pendingInboundTxs)
        allTxs.addAll(pendingOutboundTxs)
        for (tx in allTxs) {
            if (tx.user.publicKey.emojiId.contains(query)) {
                if (!users.contains(tx.user)) {
                    users.add(tx.user)
                }
            }
        }
        recyclerView.post {
            wr.get()?.displaySearchResult(users)
        }
    }

    private fun clearSearchResult() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerViewAdapter.displaySearchResult(ArrayList())
    }

    private fun displaySearchResult(users: List<User>) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
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
    @OnClick(R.id.add_recipient_btn_qr_code)
    fun onQRButtonClick() {
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        hidePasteEmojiIdViews(animate = true) {
            rootView.postDelayed({
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
                    searchEditText.setText(
                        deepLink.type.value,
                        TextView.BufferType.EDITABLE
                    )
                    searchEditText.postDelayed({
                        searchEditTextScrollView.smoothScrollTo(0, 0)
                    }, Constants.UI.mediumDurationMs)
                }
                PUBLIC_KEY_HEX -> {
                    AsyncTask.execute {
                        val publicKeyHex = deepLink.type.value
                        val publicKey = walletService.getPublicKeyFromHexString(publicKeyHex)
                        if (publicKey != null) {
                            searchEditText.post {
                                wr.get()?.searchEditText?.setText(
                                    publicKey.emojiId,
                                    TextView.BufferType.EDITABLE
                                )
                            }
                            searchEditText.postDelayed({
                                wr.get()?.searchEditTextScrollView?.smoothScrollTo(0, 0)
                            }, Constants.UI.mediumDurationMs)
                        }
                    }
                }
            }
        }
    }

    @OnClick(R.id.add_recipient_btn_continue)
    fun onContinueButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        AsyncTask.execute {
            val error = WalletError()
            val contacts = walletService.getContacts(error)
            val recipientContact: Contact? = when (error.code) {
                WalletErrorCode.NO_ERROR -> contacts.firstOrNull { it.publicKey == emojiIdPublicKey }
                else -> null
            }
            rootView.post {
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
    @OnClick(
        R.id.add_recipinet_vw_top_dimmer,
        R.id.add_recipinet_vw_middle_dimmer,
        R.id.add_recipinet_vw_bottom_dimmer
    )
    fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            val mActivity = activity
            if (mActivity != null) {
                UiUtil.hideKeyboard(mActivity)
                searchEditText.clearFocus()
            }
        }
    }

    /**
     * Paste banner clicked.
     */
    @OnClick(R.id.add_recipient_btn_paste_emoji_id)
    fun onPasteEmojiIdButtonClicked() {
        hidePasteEmojiIdViewsOnTextChanged = false
        hidePasteEmojiIdViews(animate = true) {
            searchEditText.scaleX = 0f
            searchEditText.scaleY = 0f
            searchEditText.setText(
                emojiIdPublicKey!!.emojiId,
                TextView.BufferType.EDITABLE
            )
            searchEditText.setSelection(searchEditText.text?.length ?: 0)

            rootView.postDelayed({
                animateEmojiIdPaste()
            }, Constants.UI.xShortDurationMs)
        }
    }

    private fun animateEmojiIdPaste() {
        // animate text size
        val textAnim = ValueAnimator.ofFloat(0f, 1f)
        textAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            searchEditText.scaleX = value
            searchEditText.scaleY = value
            // searchEditText.translationX = -width * (1f - value) / 2f
        }
        textAnim.duration = Constants.UI.shortDurationMs
        textAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        textAnim.start()
        rootView.postDelayed({
            searchEditTextScrollView.smoothScrollTo(0, 0)
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
        searchEditText.clearFocus()
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
        val editable = searchEditText.editableText
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

        continueButton.visibility = View.GONE
        invalidEmojiIdTextView.visibility = View.GONE

        val textWithoutSeparators = editable.toString()
        if (textWithoutSeparators.firstNCharactersAreEmojis(emojiFormatterChunkSize)) {
            searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                .withIndex()) {
                val target = index + (offset * emojiIdChunkSeparator.length)
                editable.insert(target, emojiIdChunkSeparator)
            }
            // check if valid emoji - don't search if not
            val numberofEmojis = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || numberofEmojis > emojiIdLength) {
                emojiIdPublicKey = null
                // invalid emoji-id : clear list and display error
                invalidEmojiIdTextView.text = invalidEmojiIdMessage
                invalidEmojiIdTextView.visibility = View.VISIBLE
                qrCodeButton.visibility = View.VISIBLE
                clearSearchResult()
            } else {
                if (numberofEmojis == emojiIdLength) {
                    if (textWithoutSeparators == sharedPrefsWrapper.emojiId!!) {
                        emojiIdPublicKey = null
                        invalidEmojiIdTextView.text = ownEmojiIdMessage
                        invalidEmojiIdTextView.visibility = View.VISIBLE
                        qrCodeButton.visibility = View.VISIBLE
                        clearSearchResult()
                    } else {
                        qrCodeButton.visibility = View.GONE
                        // valid emoji id length - clear list, no search, display continue button
                        AsyncTask.execute {
                            emojiIdPublicKey =
                                walletService.getPublicKeyFromEmojiId(textWithoutSeparators)
                            rootView.post {
                                if (emojiIdPublicKey == null) {
                                    invalidEmojiIdTextView.text = invalidEmojiIdMessage
                                    invalidEmojiIdTextView.visibility = View.VISIBLE
                                    clearSearchResult()
                                } else {
                                    invalidEmojiIdTextView.visibility = View.GONE
                                    continueButton.visibility = View.VISIBLE
                                    val mActivity = activity
                                    if (mActivity != null) {
                                        UiUtil.hideKeyboard(mActivity)
                                        searchEditText.clearFocus()
                                    }
                                    onSearchTextChanged(textWithoutSeparators)
                                    searchEditText.isEnabled = false
                                }
                            }
                        }
                    }
                } else {
                    emojiIdPublicKey = null
                    qrCodeButton.visibility = View.VISIBLE
                    onSearchTextChanged(textWithoutSeparators)
                }
            }
        } else {
            emojiIdPublicKey = null
            qrCodeButton.visibility = View.VISIBLE
            searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            searchEditText.letterSpacing = inputNormalLetterSpacing
            onSearchTextChanged(textWithoutSeparators)
        }
        textWatcherIsRunning = false
    }

    // endregion

    // region scroll depth gradient view controls

    private fun onRecyclerViewScrolled(totalDeltaY: Int) {
        scrollDepthView.alpha = min(
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

}