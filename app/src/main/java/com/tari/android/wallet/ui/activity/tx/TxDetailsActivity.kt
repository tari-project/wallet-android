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
package com.tari.android.wallet.ui.activity.tx

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.ActivityTxDetailsBinding
import com.tari.android.wallet.databinding.TxDetailsGifContainerBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.txFormattedDate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.model.TxStatus.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.animation.collapseAndHideAnimation
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.presentation.TxNote
import com.tari.android.wallet.ui.presentation.gif.GIF
import com.tari.android.wallet.ui.presentation.gif.GIFRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

/**
 *  Activity class - Transaction detail.
 *
 * @author The Tari Development Team
 */
internal class TxDetailsActivity : AppCompatActivity(), ServiceConnection {

    companion object {
        const val TX_EXTRA_KEY = "TX_EXTRA_KEY"
        const val TX_ID_EXTRA_KEY = "TX_DETAIL_EXTRA_KEY"

        fun createIntent(context: Context, txId: TxId): Intent {
            return Intent(context, TxDetailsActivity::class.java)
                .apply {
                    putExtra(TX_ID_EXTRA_KEY, txId)
                }
        }

        fun createIntent(context: Context, tx: Tx): Intent {
            return Intent(context, TxDetailsActivity::class.java)
                .apply {
                    putExtra(TX_EXTRA_KEY, tx)
                }
        }
    }

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var repository: GIFRepository

    private var walletService: TariWalletService? = null

    private var requiredConfirmationCount: Long = 0

    /**
     * Values below are used for scaling up/down of the text size.
     */
    private var currentTextSize = 0f
    private var currentAmountGemSize = 0f

    private lateinit var tx: Tx
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    private lateinit var ui: ActivityTxDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityTxDetailsBinding.inflate(layoutInflater).apply { setContentView(root) }
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        setupUI()

        val savedTx = savedInstanceState?.getParcelable<Tx>(TX_EXTRA_KEY)
        val intentTx = intent.getParcelableExtra<Tx>(TX_EXTRA_KEY)
        (savedTx ?: intentTx)?.let {
            tx = it
            fetchGIFIfAttached()
            bindTxData()
            observeTxUpdates()
            enableCTAs()
        }
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            Logger.d("Issuing bindService (${System.currentTimeMillis()})")
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/tx_details", title = "Transaction Details")
        }
    }

    override fun onDestroy() {
        unbindService(this)
        EventBus.unsubscribe(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(TX_EXTRA_KEY, this.tx)
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service (${System.currentTimeMillis()}).")
        val walletService = TariWalletService.Stub.asInterface(service)
        this.walletService = walletService
        if (!this::tx.isInitialized) {
            findTxAndUpdateUI(walletService)
        } else {
            fetchRequiredConfirmationCount()
            setTxStatusData(tx)
        }
    }

    private fun findTxAndUpdateUI(walletService: TariWalletService) {
        val lookedUpTx = findTxById(intent.getParcelableExtra(TX_ID_EXTRA_KEY)!!, walletService)
        if (lookedUpTx == null) {
            ErrorDialog(
                this,
                title = string(tx_details_error_tx_not_found_title),
                description = string(tx_details_error_tx_not_found_desc),
                onClose = { finish() }
            ).show()
        } else {
            tx = lookedUpTx
            fetchRequiredConfirmationCount()
            fetchGIFIfAttached()
            bindTxData()
            observeTxUpdates()
            enableCTAs()
        }
    }

    private fun fetchRequiredConfirmationCount() {
        val error = WalletError()
        val service = walletService!!
        requiredConfirmationCount = service.getRequiredConfirmationCount(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun fetchGIFIfAttached() {
        val gifId = TxNote.fromNote(tx.message).gifId ?: return
        val vm = ViewModelProvider(this)[GIFViewModel::class.java]
        vm.onGIFFetchRequested(gifId)
        GIFView(ui.gifContainer, Glide.with(this), vm, this).displayGIF()
    }

    private fun findTxById(id: TxId, walletService: TariWalletService): Tx? =
        nullOnException { walletService.getPendingInboundTxById(id, WalletError()) }
            ?: nullOnException { walletService.getPendingOutboundTxById(id, WalletError()) }
            ?: nullOnException { walletService.getCompletedTxById(id, WalletError()) }
            ?: nullOnException { walletService.getCancelledTxById(id, WalletError()) }

    private fun <T> nullOnException(supplier: () -> T): T? = try {
        supplier()
    } catch (e: Exception) {
        null
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        EventBus.unsubscribe(this)
        disableCTAs()
        walletService = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun setupUI() {
        bindViews()
        setUICommands()
        disableCTAs()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindViews() {
        currentTextSize = dimen(add_amount_element_text_size)
        currentAmountGemSize = dimen(add_amount_gem_size)

        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            this
        )
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        ui.gifContainer.root.invisible()
        ui.gifContainer.loadingGifProgressBar.setColor(color(tx_list_loading_gif_gray))
    }

    private fun disableCTAs() {
        arrayOf<TextView>(
            ui.addContactButton,
            ui.cancelTxView,
            ui.editContactLabelTextView
        ).forEach {
            it.isEnabled = false
            it.setTextColor(color(disabled_cta))
        }
    }

    private fun enableCTAs() {
        arrayOf<TextView>(ui.addContactButton, ui.cancelTxView, ui.editContactLabelTextView)
            .forEach { it.isEnabled = true }
        ui.addContactButton.setTextColor(color(purple))
        ui.editContactLabelTextView.setTextColor(color(purple))
        ui.cancelTxView.setTextColor(color(tx_detail_cancel_tx_cta))
    }

    private fun setUICommands() {
        ui.backView.setOnClickListener { onBackPressed() }
        ui.emojiIdSummaryContainerView.setOnClickListener { onEmojiSummaryClicked(it) }
        ui.feeLabelTextView.setOnClickListener { showTxFeeToolTip() }
        ui.addContactButton.setOnClickListener { onAddContactClick() }
        ui.editContactLabelTextView.setOnClickListener { onEditContactClick() }
        ui.createContactEditText.setOnEditorActionListener { _, actionId, _ ->
            onContactEditTextEditAction(actionId)
        }
        ui.cancelTxView.setOnClickListener { onTransactionCancel() }
    }

    private fun bindTxData() {
        Logger.d("Current TX: $tx")
        val tx = this.tx
        setTxStatusData(tx)
        setTxMetaData(tx)
        setTxAddresseeData(tx)
        setTxPaymentData(tx)
        setFullEmojiId(tx)
    }

    private fun setTxPaymentData(tx: Tx) {
        val state = TxState.from(tx)
        ui.amountTextView.text = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        ui.paymentStateTextView.text = when {
            tx is CancelledTx -> string(tx_detail_payment_cancelled)
            state.status == MINED_CONFIRMED || state.status == IMPORTED ->
                if (state.direction == INBOUND) string(tx_detail_payment_received)
                else string(tx_detail_payment_sent)
            else -> string(tx_detail_pending_payment_received)
        }
        when {
            tx is CompletedTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is CancelledTx && tx.direction == OUTBOUND -> setFeeData(tx.fee)
            tx is PendingOutboundTx -> setFeeData(tx.fee)
            else -> {
                ui.txFeeTextView.gone()
                ui.feeLabelTextView.gone()
            }
        }
        ui.amountContainerView.post { scaleDownAmountTextViewIfRequired() }
    }

    private fun setFullEmojiId(tx: Tx) {
        fullEmojiIdViewController.fullEmojiId = tx.user.publicKey.emojiId
        fullEmojiIdViewController.emojiIdHex = tx.user.publicKey.hexString
    }

    private fun setFeeData(fee: MicroTari) {
        ui.txFeeTextView.visible()
        ui.feeLabelTextView.visible()
        ui.txFeeTextView.text = string(
            tx_details_fee_value,
            WalletUtil.amountFormatter.format(fee.tariValue)
        )
    }

    private fun setTxMetaData(tx: Tx) {
        // display date
        ui.dateTextView.text = Date(tx.timestamp.toLong() * 1000).txFormattedDate()
        // display message
        val note = TxNote.fromNote(tx.message)
        if (note.message == null) {
            ui.txNoteTextView.gone()
        } else {
            ui.txNoteTextView.text = note.message
        }
        // display GIF
        ui.gifContainer.root.visible()
    }

    private fun setTxAddresseeData(tx: Tx) {
        val user = tx.user
        if (user is Contact) {
            ui.contactContainerView.visible()
            setUIAlias(user.alias)
        } else {
            ui.addContactButton.visible()
            ui.contactContainerView.gone()
        }
        val state = TxState.from(tx)
        ui.fromTextView.text =
            if (state.direction == INBOUND) string(common_from) else string(common_to)
        emojiIdSummaryController.display(tx.user.publicKey.emojiId)
    }

    private fun setTxStatusData(tx: Tx) {
        val state = TxState.from(tx)
        val error = WalletError()
        val requiredConfirmation = walletService?.getRequiredConfirmationCount(error) ?: 0
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        val statusText = when {
            tx is CancelledTx -> ""
            state == TxState(INBOUND, PENDING) -> string(tx_detail_waiting_for_sender_to_complete)
            state == TxState(OUTBOUND, PENDING) -> string(tx_detail_waiting_for_recipient)
            state.status != MINED_CONFIRMED -> string(
                tx_detail_completing_final_processing,
                if (tx is CompletedTx) tx.confirmationCount.toInt() + 1 else 1,
                requiredConfirmation + 1
            )
            else -> ""
        }
        ui.statusTextView.text = statusText
        ui.statusContainerView.visibility = if (statusText.isEmpty()) View.GONE else View.VISIBLE
        if (tx !is CancelledTx && state.direction == OUTBOUND && state.status == PENDING) {
            ui.cancelTxContainerView.setOnClickListener { onTransactionCancel() }
            ui.cancelTxContainerView.visible()
        } else if (ui.cancelTxContainerView.visibility == View.VISIBLE) {
            ui.cancelTxContainerView.setOnClickListener(null)
            collapseAndHideAnimation(ui.cancelTxContainerView).start()
        }
    }

    /**
     * Scales down the amount text if the amount overflows.
     */
    private fun scaleDownAmountTextViewIfRequired() {
        val contentWidthPreInsert =
            ui.amountContainerView.getLastChild()!!.right - ui.amountContainerView.getFirstChild()!!.left
        val contentWidthPostInsert = contentWidthPreInsert + ui.amountTextView.measuredWidth
        // calculate scale factor
        var scaleFactor = 1f
        while ((contentWidthPostInsert * scaleFactor) > ui.amountContainerView.width) {
            scaleFactor *= 0.95f
        }
        currentTextSize *= scaleFactor
        currentAmountGemSize *= scaleFactor

        // adjust gem size
        ui.amountGemImageView.setLayoutSize(
            currentAmountGemSize.toInt(),
            currentAmountGemSize.toInt()
        )
        ui.amountTextView.setTextSizePx(currentTextSize)
    }

    private fun observeTxUpdates() {
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) { updateTxData(it.tx) }
    }

    private fun updateTxData(tx: Tx) {
        //  Main thread invocation is necessary due to happens-before relationship guarantee
        Handler(Looper.getMainLooper()).post {
            val currentTx = this.tx
            if (tx.id == currentTx.id) {
                Logger.d("Updating TX\nOld: $currentTx\nNew: $tx")
                this.tx = tx
                bindTxData()
            }
        }
    }

    private fun onEmojiSummaryClicked(view: View) {
        view.temporarilyDisableClick()
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun onContactEditTextEditAction(actionId: Int): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val alias = ui.createContactEditText.text?.toString()
            if (!alias.isNullOrEmpty()) {
                updateContactAlias(alias)
                setUIAlias(alias)
            } else {
                removeContact()
            }
            ui.contactLabelTextView.setTextColor(color(tx_detail_contact_name_label_text))
            return false
        }
        return true
    }

    private fun onTransactionCancel() {
        val service = this.walletService ?: return
        val tx = this.tx
        if (tx is PendingOutboundTx && tx.direction == OUTBOUND && tx.status == PENDING) {
            showTxCancelDialog(service)
        } else {
            Logger.e(
                "cancelTransaction was issued, but current transaction is not pending " +
                        "outbound, but rather $tx"
            )
        }
    }

    private fun showTxCancelDialog(service: TariWalletService) {
        BottomSlideDialog(
            this,
            R.layout.dialog_cancel_tx,
            dismissViewId = R.id.cancel_tx_dialog_not_cancel_button
        ).apply {
            findViewById<Button>(R.id.cancel_tx_dialog_cancel_button)
                .setOnClickListener {
                    cancelTransaction(service)
                    dismiss()
                }
        }.show()
    }

    private fun cancelTransaction(service: TariWalletService) {
        val error = WalletError()
        val isCancelled = service.cancelPendingTx(TxId(this.tx.id), error)
        if (isCancelled || error.code == WalletErrorCode.NO_ERROR) {
            this.ui.cancelTxView.setOnClickListener(null)
        } else {
            ErrorDialog(
                this, string(tx_detail_cancellation_error_title),
                string(tx_detail_cancellation_error_description)
            ).show()
            Logger.e(
                "Error occurred during TX cancellation.\nCancelled? $isCancelled" +
                        "\nError: $error"
            )
        }
    }

    private fun removeContact() {
        val currentTx = tx
        val contact = currentTx.user as? Contact ?: return
        val error = WalletError()
        walletService?.removeContact(contact, error)
        if (error.code == WalletErrorCode.NO_ERROR) {
            currentTx.user = User(contact.publicKey)
            EventBus.post(Event.Contact.ContactRemoved(contact.publicKey))
        } else {
            TODO("Unhandled wallet error: ${error.code}")
        }
        bindTxData()
    }

    private fun updateContactAlias(newAlias: String) {
        if (walletService == null) {
            return
        }
        val error = WalletError()
        walletService?.updateContactAlias(
            tx.user.publicKey,
            newAlias,
            error
        )
        if (error.code == WalletErrorCode.NO_ERROR) {
            // update tx contact
            val contact = Contact(tx.user.publicKey, newAlias)
            tx.user = contact
            EventBus.post(
                Event.Contact.ContactAddedOrUpdated(
                    tx.user.publicKey,
                    newAlias
                )
            )
        } else {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun setUIAlias(alias: String) {
        ui.contactNameTextView.visible()
        ui.createContactEditText.invisible()
        ui.contactNameTextView.text = alias
        ui.editContactLabelTextView.visible()
        ui.addContactButton.invisible()
    }

    /**
     * Add contact alias.
     */
    private fun onAddContactClick() {
        ui.contactContainerView.visible()
        ui.addContactButton.invisible()
        ui.createContactEditText.visible()
        ui.contactLabelTextView.visible()
        ui.editContactLabelTextView.invisible()

        focusContactEditText()
    }

    private fun focusContactEditText() {
        ui.createContactEditText.post {
            ui.createContactEditText.requestFocus()
            ui.createContactEditText.setSelection(ui.createContactEditText.text?.length ?: 0)
            showKeyboard()
        }
        ui.contactLabelTextView.setTextColor(color(black))
    }

    /**
     * Edit contact alias.
     */
    private fun onEditContactClick() {
        ui.editContactLabelTextView.invisible()
        ui.createContactEditText.visible()
        focusContactEditText()
        val user = tx.user
        if (user is Contact) {
            ui.createContactEditText.setText(user.alias)
        }
        ui.contactNameTextView.invisible()
    }

    private fun showTxFeeToolTip() {
        BottomSlideDialog(
            context = this,
            layoutId = R.layout.tx_fee_tooltip_dialog,
            dismissViewId = R.id.tx_fee_tooltip_dialog_txt_close
        ).show()
    }

    class GIFViewModel() : CommonViewModel() {

        init {
            component?.inject(this)
        }

        @Inject
        lateinit var repository: GIFRepository

        private var gifId: String = ""

        private val _gif = MutableLiveData<GIFState>()
        val gif: LiveData<GIFState> get() = _gif

        val currentState get() = _gif.value!!

        init {
            onGIFFetchRequested()
        }

        fun onGIFFetchRequested(gifId: String) {
            this.gifId = gifId
            onGIFFetchRequested()
        }

        fun onGIFFetchRequested() {
            viewModelScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { _gif.value = GIFState() }
                try {
                    _gif.postValue(GIFState(repository.getById(gifId)))
                } catch (e: Exception) {
                    Logger.e(e, "Exception was thrown during gif downloading")
                    _gif.postValue(GIFState(e))
                }
            }
        }
    }

    internal data class GIFState(val gif: GIF?, val error: Exception?) {

        init {
            require(gif == null || error == null) { "Both gif and error can't be nonnull" }
        }

        constructor() : this(null, null)
        constructor(gif: GIF) : this(gif, null)
        constructor(e: Exception) : this(null, e)

        val isProcessing get() = gif == null && error == null
        val isError get() = error != null
        val isSuccessful get() = gif != null
    }

    private data class TxState(val direction: Tx.Direction, val status: TxStatus) {
        companion object {
            fun from(tx: Tx): TxState {
                return when (tx) {
                    is PendingInboundTx -> TxState(INBOUND, tx.status)
                    is PendingOutboundTx -> TxState(OUTBOUND, tx.status)
                    is CompletedTx -> TxState(tx.direction, tx.status)
                    is CancelledTx -> TxState(tx.direction, tx.status)
                    else -> throw IllegalArgumentException("Unexpected Tx type: $tx")
                }
            }
        }
    }

    private class GIFView(
        private val ui: TxDetailsGifContainerBinding,
        private val glide: RequestManager,
        private val viewModel: GIFViewModel,
        private val owner: LifecycleOwner
    ) {

        fun displayGIF() {
            viewModel.gif.observe(owner) {
                val state = it ?: return@observe
                when {
                    state.isError -> onFailure()
                    state.isSuccessful -> downloadMedia(state.gif!!)
                    state.isProcessing -> showDownloadingState()
                    else -> throw IllegalStateException()
                }
            }
            ui.retryLoadingGifTextView.setOnClickListener {
                val currentState = viewModel.currentState
                when {
                    currentState.isError -> viewModel.onGIFFetchRequested()
                    currentState.isSuccessful -> downloadMedia(currentState.gif!!)
                    else -> throw IllegalStateException()
                }
            }
        }

        private fun showDownloadingState() {
            shrinkGIF()
            hideErrorUI()
            showDownloadingUI()
        }

        private fun showErrorUI() {
            ui.gifStatusContainer.visible()
            ui.retryLoadingGifTextView.visible()
        }

        private fun hideErrorUI() {
            ui.gifStatusContainer.gone()
            ui.retryLoadingGifTextView.gone()
        }

        private fun showDownloadingUI() {
            ui.gifStatusContainer.visible()
            ui.loadingGifTextView.visible()
            ui.loadingGifProgressBar.visible()
        }

        private fun hideDownloadingUI() {
            ui.gifStatusContainer.gone()
            ui.loadingGifTextView.gone()
            ui.loadingGifProgressBar.gone()
        }

        private fun downloadMedia(gif: GIF) {
            glide.asGif()
                .load(gif.uri)
                .apply(RequestOptions().transform(RoundedCorners(10)))
                .listener(UIUpdateListener())
                .into(ui.gifContainerView)
        }

        private fun onFailure() {
            shrinkGIF()
            hideDownloadingUI()
            showErrorUI()
        }

        private fun shrinkGIF() {
            // Glide won't call the listener's methods if ImageView is gone
            ui.gifContainerView.setTopMargin(0)
        }

        private fun expandGIF() {
            val margin = ui.root.dimenPx(tx_list_item_gif_container_top_margin)
            ui.gifContainerView.setTopMargin(margin)
        }

        private inner class UIUpdateListener : RequestListener<GifDrawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<GifDrawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onFailure()
                return true
            }

            override fun onResourceReady(
                resource: GifDrawable?,
                model: Any?,
                target: Target<GifDrawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                hideDownloadingUI()
                hideErrorUI()
                expandGIF()
                return false
            }
        }

    }

}
