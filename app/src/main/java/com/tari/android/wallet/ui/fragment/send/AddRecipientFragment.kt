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
import android.content.*
import android.os.*
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.add_recipient_clipboard_emoji_id_container_height
import com.tari.android.wallet.R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.application.DeepLink.Type.EMOJI_ID
import com.tari.android.wallet.application.DeepLink.Type.PUBLIC_KEY_HEX
import com.tari.android.wallet.databinding.FragmentAddRecipientBinding
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.qr.EXTRA_QR_DATA
import com.tari.android.wallet.ui.activity.qr.QRScannerActivity
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.adapter.RecipientListAdapter
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.*
import com.tari.android.wallet.util.Constants.Wallet.emojiFormatterChunkSize
import com.tari.android.wallet.util.Constants.Wallet.emojiIdLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.min

/**
 * Fragment that manages the adding of a recipient to an outgoing transaction.
 *
 * @author The Tari Development Team
 */

class AddRecipientFragment : Fragment(),
    RecipientListAdapter.Listener,
    RecyclerView.OnItemTouchListener,
    TextWatcher,
    ServiceConnection {

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

    private lateinit var walletService: TariWalletService
    private lateinit var ui: FragmentAddRecipientBinding

    /**
     * Paste-emoji-id-related views.
     */
    private val dimmerViews
        get() = arrayOf(
            ui.topDimmerView,
            ui.middleDimmerView,
            ui.bottomDimmerView
        )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentAddRecipientBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        bindToWalletService()
        if (savedInstanceState == null) {
            tracker.screen(
                path = "/home/send_tari/add_recipient",
                title = "Send Tari - Add Recipient"
            )
        }
    }

    private fun bindToWalletService() {
        val context = requireActivity()
        val bindIntent = Intent(context, WalletService::class.java)
        context.bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // Local variable is necessary because of thread-safety guarantee
        Logger.i("AddRecipientFragment onServiceConnected")
        val walletService = TariWalletService.Stub.asInterface(service)
        this.walletService = walletService
        setupUi()
        lifecycleScope.launch(Dispatchers.IO) {
            fetchAllData(walletService)
            val clipboardHasOldEmojiId = checkClipboardForIncompatibleEmojiId()
            withContext(Dispatchers.Main) {
                displayInitialList(showKeyboard = !clipboardHasOldEmojiId)
                ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
                ui.searchEditText.addTextChangedListener(this@AddRecipientFragment)
            }
            if (clipboardHasOldEmojiId) {
                // display warning
                withContext(Dispatchers.Main) {
                    displayOldEmojiIdWarning()
                }
            } else {
                checkClipboardForValidEmojiId()
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("AddRecipientFragment onServiceDisconnected")
        // No-op
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(this)
    }

    private fun setupUi() {
        recyclerViewLayoutManager = LinearLayoutManager(requireActivity())
        ui.contactsListRecyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter = RecipientListAdapter(this)
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
        ui.contactsListRecyclerView.addOnScrollListener(scrollListener)
        ui.contactsListRecyclerView.addOnItemTouchListener(this)
        ui.scrollDepthGradientView.alpha = 0f
        UiUtil.setProgressBarColor(ui.progressBar, color(add_recipient_prog_bar))
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
        ui.emojiIdTextView.setOnClickListener { onPasteEmojiIdButtonClicked() }
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (ui.searchEditText.text.toString().isNotEmpty()) {
            ui.searchEditText.setText("")
            ui.searchEditText.isEnabled = true
            ui.continueButton.gone()
        }
    }

    private fun startQRCodeActivity() {
        val intent = Intent(activity, QRScannerActivity::class.java)
        startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
        activity?.overridePendingTransition(R.anim.slide_up, 0)
    }

    /**
     * Checks whether an emoji id from the older version is in the clipboard.
     */
    private fun checkClipboardForIncompatibleEmojiId(): Boolean {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            ?: return false

        // check for old emoji id
        val incompatibleEmojis = clipboardString.extractEmojis(emojiSet = EmojiUtil.oldEmojiSet)
        return incompatibleEmojis.size >= emojiIdLength
    }

    /**
     * Checks clipboard data for a valid deep link or an emoji id.
     */
    private fun checkClipboardForValidEmojiId() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            ?: return

        val deepLink = DeepLink.from(clipboardString)
        if (deepLink != null) { // there is a deep link in the clipboard
            emojiIdPublicKey = when (deepLink.type) {
                EMOJI_ID -> walletService.getPublicKeyFromEmojiId(deepLink.identifier)
                PUBLIC_KEY_HEX -> walletService.getPublicKeyFromHexString(deepLink.identifier)
            }
        } else { // try to extract a valid emoji id
            val emojis = clipboardString.trim().extractEmojis()
            // search in windows of length = emoji id length
            var currentIndex = emojis.size - emojiIdLength
            while (currentIndex >= 0) {
                val emojiWindow =
                    emojis
                        .subList(currentIndex, currentIndex + emojiIdLength)
                        .joinToString(separator = "")
                // there is a chunked emoji id in the clipboard
                emojiIdPublicKey = walletService.getPublicKeyFromEmojiId(emojiWindow)
                if (emojiIdPublicKey != null) {
                    break
                }
                --currentIndex
            }
        }
        if (emojiIdPublicKey == null) {
            checkClipboardForPublicKeyHex(clipboardString)
        }
        emojiIdPublicKey?.let {
            if (it.emojiId != sharedPrefsWrapper.emojiId!!) {
                ui.rootView.postDelayed({
                    hidePasteEmojiIdViewsOnTextChanged = true
                    showPasteEmojiIdViews(it)
                    focusEditTextAndShowKeyboard()
                }, 100L)
            }

        }
    }

    /**
     * Checks clipboard data for a public key hex string.
     */
    private fun checkClipboardForPublicKeyHex(clipboardString: String) {
        val hexStringRegex = Regex("([A-Za-z0-9]{64})")
        var result = hexStringRegex.find(clipboardString)
        while (result != null) {
            val hexString = result.value
            emojiIdPublicKey = walletService.getPublicKeyFromHexString(hexString)
            if (emojiIdPublicKey != null) {
                return
            }
            result = result.next()
        }
    }

    /**
     * Displays paste-emoji-id-related views.
     */
    private fun showPasteEmojiIdViews(publicKey: PublicKey) {
        ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            publicKey.emojiId,
            string(emoji_id_chunk_separator),
            color(black),
            color(light_gray)
        )
        UiUtil.setBottomMargin(
            ui.emojiIdContainerView,
            -dimenPx(add_recipient_clipboard_emoji_id_container_height)
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
                (-dimenPx(add_recipient_clipboard_emoji_id_container_height) * (1f - animValue)).toInt()
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
                (dimenPx(add_recipient_paste_emoji_id_button_visible_top_margin) * value).toInt()
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
                (dimenPx(add_recipient_paste_emoji_id_button_visible_top_margin) * (1 - value)).toInt()
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
                (-dimenPx(add_recipient_clipboard_emoji_id_container_height) * value).toInt()
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

    private fun displayOldEmojiIdWarning() {
        BottomSlideDialog(
            context = activity ?: return,
            layoutId = R.layout.incompatible_emoji_id_dialog,
            dismissViewId = R.id.incompatible_emoji_id_dialog_txt_close
        ).show()
    }

    /**
     * Called only once in onCreate.
     */
    private fun fetchAllData(walletService: TariWalletService) {
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
    }

    /**
     * Displays non-search-result list.
     */
    private fun displayInitialList(showKeyboard: Boolean) {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displayList(recentTxUsers, contacts)

        if (showKeyboard) { // show keyboard
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
            displayInitialList(showKeyboard = false)
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
            ui.rootView.postDelayed({ startQRCodeActivity() }, Constants.UI.keyboardHideWaitMs)
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
                        deepLink.identifier,
                        TextView.BufferType.EDITABLE
                    )
                    ui.searchEditText.postDelayed({
                        ui.searchEditTextScrollView.smoothScrollTo(0, 0)
                    }, Constants.UI.mediumDurationMs)
                }
                PUBLIC_KEY_HEX -> {
                    AsyncTask.execute {
                        val publicKeyHex = deepLink.identifier
                        val publicKey = walletService.getPublicKeyFromHexString(publicKeyHex)
                        if (publicKey != null) {
                            ui.rootView.post {
                                ui.searchEditText.setText(
                                    publicKey.emojiId,
                                    TextView.BufferType.EDITABLE
                                )
                            }
                            ui.searchEditText.postDelayed({
                                ui.searchEditTextScrollView.smoothScrollTo(0, 0)
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
            ui.rootView.postDelayed({ animateEmojiIdPaste() }, Constants.UI.xShortDurationMs)
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
            if (count == 1 && after == 0 && s[start].toString() == string(emoji_id_chunk_separator)) {
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
        val separator = string(emoji_id_chunk_separator)
        for ((offset, index) in EmojiUtil.getExistingChunkSeparatorIndices(
            text,
            separator
        ).withIndex()) {
            val target = index - (offset * separator.length)
            editable.delete(target, target + separator.length)
        }

        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()

        val textWithoutSeparators = editable.toString()
        if (textWithoutSeparators.firstNCharactersAreEmojis(emojiFormatterChunkSize)) {
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                .withIndex()) {
                val chunkSeparatorSpannable = EmojiUtil.getChunkSeparatorSpannable(
                    separator,
                    color(light_gray)
                )
                val target = index + (offset * separator.length)
                editable.insert(target, chunkSeparatorSpannable)
            }
            // check if valid emoji - don't search if not
            val numberofEmojis = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || numberofEmojis > emojiIdLength) {
                emojiIdPublicKey = null
                // invalid emoji-id : clear list and display error
                val incompatibleEmojis = text.extractEmojis(emojiSet = EmojiUtil.oldEmojiSet)
                ui.invalidEmojiIdTextView.text = string(
                    if (incompatibleEmojis.size >= emojiIdLength) {
                        add_recipient_incompatible_emoji_id_desc_short
                    } else {
                        add_recipient_invalid_emoji_id
                    }
                )
                ui.invalidEmojiIdTextView.visible()
                ui.qrCodeButton.visible()
                clearSearchResult()
            } else {
                if (numberofEmojis == emojiIdLength) {
                    if (textWithoutSeparators == sharedPrefsWrapper.emojiId!!) {
                        emojiIdPublicKey = null
                        ui.invalidEmojiIdTextView.text = string(add_recipient_own_emoji_id)
                        ui.invalidEmojiIdTextView.visible()
                        ui.qrCodeButton.visible()
                        clearSearchResult()
                    } else {
                        ui.qrCodeButton.gone()
                        // valid emoji id length - clear list, no search, display continue button
                        lifecycleScope.launch(Dispatchers.IO) {
                            emojiIdPublicKey =
                                walletService.getPublicKeyFromEmojiId(textWithoutSeparators)
                            withContext(Dispatchers.Main) {
                                if (emojiIdPublicKey == null) {
                                    ui.invalidEmojiIdTextView.text =
                                        string(add_recipient_invalid_emoji_id)
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
            emojiIdPublicKey = null
            val incompatibleEmojis = text.extractEmojis(emojiSet = EmojiUtil.oldEmojiSet)
            if (incompatibleEmojis.size >= emojiIdLength) {
                ui.invalidEmojiIdTextView.text = string(add_recipient_incompatible_emoji_id_desc_short)
                ui.invalidEmojiIdTextView.visible()
            }
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
            totalDeltaY / (dimenPx(R.dimen.add_recipient_contact_list_item_height)).toFloat()
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
