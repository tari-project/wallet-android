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

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.*
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.send.adapter.RecipientListAdapter
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.Wallet.emojiFormatterChunkSize
import com.tari.android.wallet.util.Constants.Wallet.emojiIdLength
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.numberOfEmojis
import com.tari.android.wallet.util.firstNCharactersAreEmojis
import java.lang.ref.WeakReference
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
    @BindView(R.id.add_recipient_txt_search)
    lateinit var searchEditText: EditText
    @BindView(R.id.add_recipient_txt_title)
    lateinit var titleTextView: TextView
    @BindView(R.id.add_recipient_btn_continue)
    lateinit var continueButton: Button
    @BindView(R.id.add_recipient_txt_invalid_emoji_id)
    lateinit var invalidEmojiIdTextView: TextView
    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String

    /**
     * Paste-emoji-id-related views.
     */
    @BindViews(
        R.id.add_recipinet_vw_top_dimmer,
        R.id.add_recipinet_vw_middle_dimmer,
        R.id.add_recipinet_vw_bottom_dimmer
    )
    lateinit var dimmerViews: List<@JvmSuppressWildcards View>
    @BindView(R.id.add_recipient_vw_paste_emoji_id_banner)
    lateinit var pasteEmojiBannerView: View
    @BindView(R.id.add_recipient_txt_clipboard_emoji_id)
    lateinit var pasteEmojiClipboardTextView: TextView

    @BindDimen(R.dimen.add_recipient_contact_list_item_height)
    @JvmField
    var listItemHeight = 0
    @BindColor(R.color.add_recipient_prog_bar)
    @JvmField
    var progressBarColor = 0

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
        hidePasteEmojiIdViews()

        continueButton.visibility = View.GONE
        invalidEmojiIdTextView.visibility = View.GONE
        searchEditText.addTextChangedListener(this)

        AsyncTask.execute {
            wr.get()?.fetchRecentTxUsers()
        }
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (searchEditText.text.toString().isNotEmpty()) {
            searchEditText.setText("")
            continueButton.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        AsyncTask.execute {
            wr.get()?.checkClipboardForValidEmojiId()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    /**
     * Checks whether an emoji id is in the clipboard data.
     * Currently checks for a single emoji.
     */
    private fun checkClipboardForValidEmojiId(): Boolean {
        val clipboardManager =
            (activity?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager) ?: return false
        val clipboardString =
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return false
        // if clipboard contains at least 1 emoji, then display paste emoji banner
        if (clipboardString.numberOfEmojis() > 0) {
            emojiIdPublicKey = walletService.getPublicKeyForEmojiId(clipboardString)
            if (emojiIdPublicKey != null) {
                recyclerView.post {
                    wr.get()?.displayPasteEmojiIdViews()
                    wr.get()?.focusEditTextAndShowKeyboard()
                }
                return true
            }
            return false
        } else {
            recyclerView.post {
                wr.get()?.hidePasteEmojiIdViews()
            }
            return false
        }
    }

    /**
     * Hides paste-emoji-id-related views.
     */
    private fun hidePasteEmojiIdViews() {
        pasteEmojiBannerView.visibility = View.GONE
        dimmerViews.forEach {
            it.visibility = View.GONE
        }
    }

    /**
     * Displays paste-emoji-id-related views.
     */
    private fun displayPasteEmojiIdViews() {
        // TODO this will change once emoji id display spec is clear
        val shortenedEmojiId = EmojiUtil.getShortenedEmojiId(emojiIdPublicKey!!.emojiId)
        pasteEmojiClipboardTextView.text = shortenedEmojiId
        pasteEmojiBannerView.visibility = View.VISIBLE
        dimmerViews.forEach {
            it.visibility = View.VISIBLE
        }
    }

    /**
     * Fetches users with whom this wallet had recent transactions.
     */
    private fun fetchRecentTxUsers() {
        val users = walletService.getRecentTxUsers(recentTxContactsLimit)
        val contacts = walletService.contacts
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
        val users = ArrayList<User>()
        // get all contacts and filter by alias and emoji id
        val filteredContacts = walletService.contacts.filter {
            it.alias.contains(query, ignoreCase = true) || it.publicKey.emojiId.contains(query)
        }
        users.addAll(filteredContacts)
        val allTxs = ArrayList<Tx>()
        allTxs.addAll(walletService.completedTxs)
        allTxs.addAll(walletService.pendingInboundTxs)
        allTxs.addAll(walletService.pendingOutboundTxs)
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

    @OnClick(R.id.add_recipient_btn_continue)
    fun onContinueButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listenerWR.get()?.continueToAmount(
            this,
            User(emojiIdPublicKey!!)
        )
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
        hidePasteEmojiIdViews()
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        searchEditText.clearFocus()
    }

    /**
     * Paste banner clicked.
     */
    @OnClick(R.id.add_recipient_vw_paste_emoji_id_banner)
    fun onEmojiIdBannerClicked() {
        pasteEmojiBannerView.visibility = View.GONE
        searchEditText.setText(emojiIdPublicKey!!.emojiId, TextView.BufferType.EDITABLE)
        dimmerViews.forEach {
            it.visibility = View.GONE
        }
        val mActivity = activity ?: return
        UiUtil.hideKeyboard(mActivity)
        searchEditText.clearFocus()
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
        if (textWatcherIsRunning) {
            return
        }
        textWatcherIsRunning = true

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
            editable.delete(target, target + 1)
        }

        continueButton.visibility = View.GONE
        invalidEmojiIdTextView.visibility = View.GONE

        val textWithoutSeparators = editable.toString()
        if (textWithoutSeparators.firstNCharactersAreEmojis(emojiFormatterChunkSize)) {
            searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators).withIndex()) {
                val target = index + (offset * emojiIdChunkSeparator.length)
                editable.insert(target, emojiIdChunkSeparator)
            }
            // check if valid emoji - don't search if not
            val numberofEmojis = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || numberofEmojis > emojiIdLength) {
                // invalid emoji-id : clear list and display error
                invalidEmojiIdTextView.visibility = View.VISIBLE
                displaySearchResult(ArrayList())
            } else if (numberofEmojis == emojiIdLength) {
                // valid emoji id - clear list, no search, display continue button
                continueButton.visibility = View.VISIBLE
                val mActivity = activity
                if (mActivity != null) {
                    UiUtil.hideKeyboard(mActivity)
                    searchEditText.clearFocus()
                }
                displaySearchResult(ArrayList())
            } else {
                // valid emoji, search
                onSearchTextChanged(textWithoutSeparators)
            }
        } else {
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