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
import androidx.lifecycle.*
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
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.yat.YatService
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.yat.EmojiId
import com.tari.android.wallet.model.yat.EmojiSet
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.qr.EXTRA_QR_DATA
import com.tari.android.wallet.ui.activity.qr.QRScannerActivity
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.adapter.RecipientListAdapter
import com.tari.android.wallet.util.*
import com.tari.android.wallet.util.Constants.Wallet.emojiFormatterChunkSize
import com.tari.android.wallet.util.Constants.Wallet.emojiIdLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import javax.inject.Inject
import kotlin.math.min

/**
 * => text input'un durumuna göre aramanı yap & display et
 *
 * search: contact alias, emoji id, yat :: birinden biri match ediyorsa torbaya ekle
 * display: alias varsa alias, yoksa yat, yoksa emoji id
 *
 * click edilince: tx seçiliyse oradan yürü, contact seçiliyse yat ara ve attach et
 * (user'ı geçiyorsun, yat'ı da ayrıca ekle (bunları tek bir nesneye indirebilirsin - Tx?))
 *
 * keep data in view model
 * live data :: text input, search result
 */

/**
 * Fragment that manages the adding of a recipient to an outgoing transaction.
 *
 * @author The Tari Development Team
 */
class AddRecipientFragment : Fragment(),
    RecyclerView.OnItemTouchListener,
    TextWatcher,
    ServiceConnection {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var yatService: YatService

    @Inject
    lateinit var yatUserStorage: YatUserStorage

    @Inject
    lateinit var yatEmojiSet: EmojiSet

    private lateinit var recyclerViewAdapter: RecipientListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private var scrollListener = ScrollListener(this::onRecyclerViewScrolled)

    private var recentTxUsersLimit = 3
    private val allTxs = ArrayList<Tx>()
    private val contacts = ArrayList<Contact>()

    // Will be non-null if there's a valid emoji id in the clipboard
    private var emojiIdPublicKey: PublicKey? = null

    // Fields related to emoji id input masking.
    private var isDeletingSeparatorAtIndex: Int? = null
    private var textWatcherIsRunning = false

    private val textChangedProcessHandler = Handler(Looper.getMainLooper())
    private var textChangedProcessRunnable = Runnable { processTextChanged() }

    private var hidePasteEmojiIdViewsOnTextChanged = false

    private lateinit var walletService: TariWalletService
    private lateinit var sendToSummaryController: EmojiIdSummaryViewController
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddRecipientBinding.inflate(
        inflater, container,
        false
    ).also {
        ui = it
    }.root

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
        lifecycleScope.launch(Dispatchers.Main) {
            setupUI()
            withContext(Dispatchers.IO) { fetchAllData(walletService) }
            displayInitialList()
            ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            ui.searchEditText.addTextChangedListener(this@AddRecipientFragment)
            checkClipboardForValidEmojiIdOrYat()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("AddRecipientFragment onServiceDisconnected")
        // No-op
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModelStore.clear()
        requireActivity().unbindService(this)
    }

    private fun setupUI() {
        sendToSummaryController =
            EmojiIdSummaryViewController(ui.sendToEmojiSummaryView, yatEmojiSet)
        recyclerViewLayoutManager = LinearLayoutManager(requireActivity())
        ui.contactsListRecyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter = RecipientListAdapter(yatEmojiSet) { user ->
            if (user is Contact) { // no yat info stored for contacts, so check if there's a yat
                user.yat = allTxs.firstOrNull {
                    it.user.publicKey == user.publicKey && it.user.yat != null
                }?.user?.yat
            }
            (activity as? Listener)?.continueToAmount(this, user)
        }
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
        ui.contactsListRecyclerView.addOnScrollListener(scrollListener)
        ui.contactsListRecyclerView.addOnItemTouchListener(this)
        ui.scrollDepthGradientView.alpha = 0f
        ui.progressBar.setColor(color(add_recipient_prog_bar))
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
        ui.yatEmojiIdTextView.setOnClickListener { onPasteEmojiIdButtonClicked() }
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
     * Checks clipboard data for a valid deep link or an emoji id.
     */
    private fun checkClipboardForValidEmojiIdOrYat() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            ?: return

        val deepLink = DeepLink.from(clipboardString)
        val yat: EmojiId? =
            deepLink?.yat(yatEmojiSet) ?: EmojiId.of(clipboardString.trim(), yatEmojiSet)
        if (yat != null) {
            if (yatUserStorage.get()!!.emojiIds.all { it != yat }) {
                ui.addRecipientTxtPasteEmojiId.text = string(paste_yat)
                ui.yatEmojiIdTextView.visible()
                ui.emojiIdScrollView.gone()
                ui.yatEmojiIdTextView.text = yat.raw
                ui.rootView.postDelayed(100L) {
                    hidePasteEmojiIdViewsOnTextChanged = true
                    showPasteEmojiIdViews()
                    focusEditTextAndShowKeyboard()
                }
            }
        } else {
            parseOldLinkType(deepLink, clipboardString)
        }
    }

    private fun parseOldLinkType(deepLink: DeepLink?, clipboardString: String) {
        // there is a deep link in the clipboard
        if (deepLink != null) {
            emojiIdPublicKey = when (deepLink.type) {
                EMOJI_ID -> walletService.getPublicKeyFromEmojiId(deepLink.identifier)
                PUBLIC_KEY_HEX -> walletService.getPublicKeyFromHexString(deepLink.identifier)
            }
        } else {
            // try to extract a valid emoji id
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
                if (emojiIdPublicKey != null) break
                --currentIndex
            }
        }
        if (emojiIdPublicKey == null) checkClipboardForPublicKeyHex(clipboardString)
        emojiIdPublicKey?.let {
            ui.addRecipientTxtPasteEmojiId.text = string(paste_emoji_id)
            ui.yatEmojiIdTextView.gone()
            ui.emojiIdScrollView.visible()
            if (it.emojiId != sharedPrefsWrapper.emojiId!!) {
                ui.rootView.postDelayed(100L) {
                    hidePasteEmojiIdViewsOnTextChanged = true
                    ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
                        it.emojiId,
                        string(emoji_id_chunk_separator),
                        color(black),
                        color(light_gray)
                    )
                    showPasteEmojiIdViews()
                    focusEditTextAndShowKeyboard()
                }
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
    private fun showPasteEmojiIdViews() {
        ui.emojiIdContainerView.visible()
        ui.pasteEmojiIdContainerView.setTopMargin(0)
        ui.emojiIdContainerView.setBottomMargin(
            -dimenPx(add_recipient_clipboard_emoji_id_container_height)
        )
        dimmerViews.forEach { dimmerView ->
            dimmerView.visible()
            dimmerView.alpha = 0f
        }

        val emojiIdAppearAnim = animateValues(
            values = floatArrayOf(0F, 1F),
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO),
            duration = Constants.UI.mediumDurationMs,
            onUpdate = { valueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                dimmerViews.forEach { it.alpha = animValue * 0.6f }
                ui.emojiIdContainerView.setBottomMargin(
                    (-dimenPx(add_recipient_clipboard_emoji_id_container_height) * (1F - animValue)).toInt()
                )
            }
        )
        val pasteButtonAppearAnim = animateValues(
            values = floatArrayOf(0F, 1F),
            interpolator = EasingInterpolator(Ease.BACK_OUT),
            duration = Constants.UI.shortDurationMs,
            onStart = { ui.pasteEmojiIdContainerView.visible() },
            onUpdate = { valueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (dimenPx(add_recipient_paste_emoji_id_button_visible_top_margin) * value).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = value
            }
        )

        val animSet = animatorSetOf(startDelay = Constants.UI.xShortDurationMs)
        animSet.playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
        animSet.start()
    }

    private fun hidePasteEmojiIdViews(animate: Boolean, onEnd: () -> Unit = {}) {
        if (!animate) {
            ui.pasteEmojiIdContainerView.gone()
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
            onEnd()
            return
        }
        // animate and hide paste emoji id button
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.pasteEmojiIdContainerView.setTopMargin(
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
            ui.emojiIdContainerView.setBottomMargin(
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
        animSet.addListener(onEnd = { onEnd() })
        animSet.start()
    }

    // Called only once in onCreate.
    private fun fetchAllData(walletService: TariWalletService) {
        val error = WalletError()
        // get contacts
        contacts.repopulate(
            walletService.getContacts(error) ?: emptyList()
        )
        // get all txs
        allTxs.addAll(walletService.getCompletedTxs(error) ?: emptyList())
        allTxs.addAll(walletService.getPendingInboundTxs(error) ?: emptyList())
        allTxs.addAll(walletService.getPendingOutboundTxs(error) ?: emptyList())
        allTxs.addAll(walletService.getCancelledTxs(error) ?: emptyList())
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        allTxs.sortByDescending { it.timestamp }
    }

    // Displays non-search-result list.
    private fun displayInitialList() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displayList(
            allTxs.distinctBy { it.user }.take(recentTxUsersLimit).map { it.user },
            contacts
        )
        focusEditTextAndShowKeyboard()
    }

    private fun focusEditTextAndShowKeyboard() {
        val activity = activity ?: return
        ui.searchEditText.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }


    private fun onSearchTextChanged(query: String) {
        if (query.isEmpty()) {
            displayInitialList()
        } else {
            val users = searchRecipients(query)
            if (users.isEmpty()) {
                val yat = EmojiId.of(query, yatEmojiSet)
                if (yat != null) {
                    ui.noWalletForYatTextView.gone()
                    ui.progressBar.gone()
                    ui.contactsListRecyclerView.invisible()
                    ui.sendToYatCtaContainer.visible()
                    sendToSummaryController.display(yat.raw)
                    ui.sendToYatCtaContainer.setOnClickListener {
                        ui.searchEditText.isEnabled = false
                        lifecycleScope.launch(Dispatchers.IO) { lookupYat(yat) }
                    }
                } else {
                    ui.noWalletForYatTextView.gone()
                    ui.sendToYatCtaContainer.gone()
                    ui.progressBar.gone()
                    ui.contactsListRecyclerView.visible()
                    recyclerViewAdapter.displaySearchResult(emptyList())
                }
            } else {
                ui.noWalletForYatTextView.gone()
                ui.sendToYatCtaContainer.gone()
                ui.progressBar.gone()
                ui.contactsListRecyclerView.visible()
                recyclerViewAdapter.displaySearchResult(users)
            }
        }
    }

    private fun searchRecipients(query: String): List<User> {
        // search transaction users
        val filteredTxUsers = allTxs.filter {
            it.user.publicKey.emojiId.contains(query)
                    || (it.user as? Contact)?.alias?.contains(query, ignoreCase = true) ?: false
                    || it.user.yat?.contains(query) ?: false
        }.map { it.user }.distinct()

        // search contacts (we don't have non-transaction contacts at the moment, but we probably
        // will have them in the future - so this is a safety measure)
        val filteredContacts = contacts.filter {
            it.publicKey.emojiId.contains(query)
                    || it.alias.contains(query, ignoreCase = true)
        }
        return (filteredTxUsers + filteredContacts).distinct().sortedWith { o1, o2 ->
            val value1 = ((o1 as? Contact)?.alias ?: o1.yat) ?: o1.publicKey.emojiId
            val value2 = ((o2 as? Contact)?.alias ?: o2.yat) ?: o2.publicKey.emojiId
            value1.compareTo(value2)
        }
    }

    private suspend fun lookupYat(yat: EmojiId) {
        withContext(Dispatchers.Main) {
            ui.sendToYatCtaContainer.isClickable = false
            ui.noWalletForYatTextView.gone()
            ui.contactsListRecyclerView.invisible()
            scrollListener.reset()
            ui.scrollDepthGradientView.alpha = 0f
            ui.progressBar.visible()
        }

        try {
            val user = yatService.getUser(yat) ?: throw UserNotFoundException(yat.raw)
            withContext(Dispatchers.Main) { onUserFoundByYat(user) }
        } catch (exception: UserNotFoundException) {
            withContext(Dispatchers.Main) { onWalletDoesNotExistForYat(yat) }
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) { onYatLookupError() }
        }
    }

    private fun onWalletDoesNotExistForYat(id: EmojiId) {
        ui.sendToYatCtaContainer.isClickable = false
        ui.searchEditText.isEnabled = true
        ui.sendToYatCtaContainer.gone()
        ui.progressBar.gone()
        ui.contactsListRecyclerView.invisible()
        ui.noWalletForYatTextView.visible()
        ui.noWalletForYatTextView.text = string(add_recipient_error_no_wallet_for_yat, id.raw)
    }

    private fun onUserFoundByYat(user: User) {
        ui.sendToYatCtaContainer.isClickable = false
        ui.searchEditText.isEnabled = true
        ui.noWalletForYatTextView.gone()
        ui.sendToYatCtaContainer.gone()
        ui.progressBar.gone()
        ui.contactsListRecyclerView.invisible()
        (requireActivity() as Listener).continueToAmount(this, user)
    }

    private fun onYatLookupError() {
        ErrorDialog(
            requireContext(),
            string(add_recipient_search_error_title),
            string(add_recipient_search_error_description)
        ).show()
        ui.sendToYatCtaContainer.isClickable = true
        ui.searchEditText.isEnabled = true
        ui.progressBar.gone()
        ui.noWalletForYatTextView.gone()
        ui.sendToYatCtaContainer.gone()
        ui.contactsListRecyclerView.visible()
        scrollListener.reset()
        ui.scrollDepthGradientView.alpha = 0f
    }

    private fun onBackButtonClicked(view: View) {
        ui.rootView.postDelayed(200L) { requireActivity().onBackPressed() }
        view.temporarilyDisableClick()
        activity?.let {
            it.hideKeyboard()
            ui.rootView.postDelayed(200L) { it.onBackPressed() }
        }
    }

    private fun clearSearchResult() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        recyclerViewAdapter.displaySearchResult(ArrayList())
    }

    /**
     * Open QR code scanner on button click.
     */
    private fun onQRButtonClick() {
        requireActivity().hideKeyboard()
        hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER
            && resultCode == Activity.RESULT_OK
            && data != null
        ) {
            val qrData = data.getStringExtra(EXTRA_QR_DATA) ?: return
            val deepLink = DeepLink.from(qrData) ?: return
            val yat = deepLink.yat(yatEmojiSet)
            if (yat == null) {
                when (deepLink.type) {
                    EMOJI_ID -> {
                        ui.searchEditText.setText(
                            deepLink.identifier,
                            TextView.BufferType.EDITABLE
                        )
                        ui.searchEditText.postDelayed(Constants.UI.mediumDurationMs) {
                            ui.searchEditTextScrollView.smoothScrollTo(0, 0)
                        }
                    }
                    PUBLIC_KEY_HEX -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val publicKeyHex = deepLink.identifier
                            val publicKey = walletService.getPublicKeyFromHexString(publicKeyHex)
                            if (publicKey != null) {
                                withContext(Dispatchers.Main) {
                                    ui.searchEditText.setText(
                                        publicKey.emojiId,
                                        TextView.BufferType.EDITABLE
                                    )
                                }
                                ui.searchEditText.postDelayed(Constants.UI.mediumDurationMs) {
                                    ui.searchEditTextScrollView.smoothScrollTo(0, 0)
                                }
                            }
                        }
                    }
                }
            } else {
                ui.searchEditText.setText(yat.raw)
            }
        }
    }

    private fun onContinueButtonClicked(view: View) {
        view.temporarilyDisableClick()
        lifecycleScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val contacts = walletService.getContacts(error)
            val recipientContact: Contact? = when (error.code) {
                WalletErrorCode.NO_ERROR -> contacts.firstOrNull { it.publicKey == emojiIdPublicKey }
                else -> null
            }
            withContext(Dispatchers.Main) {
                (activity as? Listener)?.continueToAmount(
                    this@AddRecipientFragment,
                    recipientContact ?: User(emojiIdPublicKey!!)
                )
            }
        }
    }

    private fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            requireActivity().hideKeyboard()
            ui.searchEditText.clearFocus()
        }
    }

    private fun onPasteEmojiIdButtonClicked() {
        hidePasteEmojiIdViewsOnTextChanged = false
        hidePasteEmojiIdViews(animate = true) {
            ui.searchEditText.scaleX = 0f
            ui.searchEditText.scaleY = 0f
            val pubkey = emojiIdPublicKey
            if (pubkey == null) {
                ui.searchEditText.setText(
                    ui.yatEmojiIdTextView.text.toString(),
                    TextView.BufferType.EDITABLE
                )
            } else {
                ui.searchEditText.setText(pubkey.emojiId, TextView.BufferType.EDITABLE)
                emojiIdPublicKey = null
            }
            ui.searchEditText.setSelection(ui.searchEditText.text?.length ?: 0)
            ui.rootView.postDelayed(Constants.UI.xShortDurationMs) { animateEmojiIdPaste() }
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
        ui.rootView.postDelayed(Constants.UI.shortDurationMs) {
            ui.searchEditTextScrollView.smoothScrollTo(0, 0)
        }
    }

    // region recycler view item touch listener

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // no-op
    }

    /**
     * Hide keyboard on list touch.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        requireActivity().hideKeyboard()
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
            if (count == 1 && after == 0 && s[start].toString() == string(emoji_id_chunk_separator)) start else null
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // no-op
    }

    override fun afterTextChanged(editable: Editable) {
        if (hidePasteEmojiIdViewsOnTextChanged) {
            hidePasteEmojiIdViews(animate = true)
            hidePasteEmojiIdViewsOnTextChanged = false
        }
        if (textWatcherIsRunning) return
        textChangedProcessHandler.removeCallbacks(textChangedProcessRunnable)
        textChangedProcessHandler.postDelayed(textChangedProcessRunnable, 100L)
    }

    private fun processTextChanged() {
        textWatcherIsRunning = true
        val editable = ui.searchEditText.editableText
        val text = editable.toString()

        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()

        val separator = string(emoji_id_chunk_separator)
		val textWithoutSeparators = text.replace(separator, "")
		val yatEmojiId = EmojiId.of(textWithoutSeparators, yatEmojiSet)?.raw
        if (yatEmojiId != null ||
            textWithoutSeparators.firstNCharactersAreEmojis(emojiFormatterChunkSize)
        ) {
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = if (yatEmojiId == null) 0.27F else 0.04F

            val deleteIndices = EmojiUtil.getExistingChunkSeparatorIndices(text, separator)
            for ((offset, index) in deleteIndices.withIndex()) {
                val target = index - offset * separator.length
                editable.delete(target, target + separator.length)
            }
            if (yatEmojiId == null) {
                val insertIndices = EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                for ((offset, index) in insertIndices.withIndex()) {
                    val chunkSeparatorSpannable =
                        EmojiUtil.getChunkSeparatorSpannable(separator, color(light_gray))
                    editable.insert(
                        index + (offset * separator.length),
                        chunkSeparatorSpannable
                    )
                }
            }
            // check if valid emoji - don't search if not
            val numberOfEmojis = textWithoutSeparators.numberOfEmojis()
            if (yatEmojiId == null
                && (textWithoutSeparators.containsNonEmoji() || numberOfEmojis > emojiIdLength)) {
                emojiIdPublicKey = null
                // invalid emoji-id : clear list and display error
                ui.invalidEmojiIdTextView.text = string(add_recipient_invalid_emoji_id)
                ui.invalidEmojiIdTextView.visible()
                ui.qrCodeButton.visible()
                clearSearchResult()
            } else {
                if (numberOfEmojis == emojiIdLength || yatEmojiId != null) {
                    if (textWithoutSeparators == sharedPrefsWrapper.emojiId!! ||
                        (yatEmojiId != null && yatUserStorage.get()!!.emojiIds.map(EmojiId::raw)
                            .any { it == yatEmojiId })
                    ) {
                        emojiIdPublicKey = null
                        ui.invalidEmojiIdTextView.text = string(add_recipient_own_emoji_id)
                        ui.invalidEmojiIdTextView.visible()
                        ui.qrCodeButton.visible()
                        clearSearchResult()
                    } else if (yatEmojiId == null) {
                        ui.qrCodeButton.gone()
                        // valid emoji id length - clear list, no search, display continue button
                        lifecycleScope.launch(Dispatchers.Main) {
                            emojiIdPublicKey =
                                walletService.getPublicKeyFromEmojiId(textWithoutSeparators)
                            if (emojiIdPublicKey == null) {
                                ui.invalidEmojiIdTextView.text =
                                    string(add_recipient_invalid_emoji_id)
                                ui.invalidEmojiIdTextView.visible()
                                clearSearchResult()
                            } else {
                                ui.invalidEmojiIdTextView.gone()
                                ui.continueButton.visible()
                                activity?.hideKeyboard()
                                ui.searchEditText.clearFocus()
                                onSearchTextChanged(textWithoutSeparators)
                                ui.searchEditText.isEnabled = false
                            }
                        }
                    } else {
                        onSearchTextChanged(textWithoutSeparators)
                    }
                } else {
                    emojiIdPublicKey = null
                    ui.qrCodeButton.visible()
                    onSearchTextChanged(textWithoutSeparators)
                }
            }
        } else {
            emojiIdPublicKey = null
            ui.qrCodeButton.visible()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            ui.searchEditText.letterSpacing = 0.04f
            onSearchTextChanged(text)
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

    class ScrollListener(private val onScrolled: (Int) -> Unit) : RecyclerView.OnScrollListener() {

        private var totalDeltaY = 0

        fun reset() {
            totalDeltaY = 0
        }

        override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
            super.onScrolled(recyclerView, dX, dY)
            totalDeltaY += dY
            onScrolled(totalDeltaY)
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