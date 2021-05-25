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
package com.tari.android.wallet.ui.fragment.tx

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.FragmentTxListBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.service.connection.TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED
import com.tari.android.wallet.ui.activity.debug.DebugActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomTypefaceSpan
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.FinalizeSendTxFragment
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListAdapter
import com.tari.android.wallet.ui.presentation.gif.GIFRepository
import com.tari.android.wallet.ui.resource.AnimationResource
import com.tari.android.wallet.ui.resource.ResourceContainer
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

internal class TxListFragment : Fragment(),
    View.OnScrollChangeListener,
    View.OnTouchListener,
    CustomScrollView.Listener,
    UpdateProgressViewController.Listener,
    ShakeDetector.Listener {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var repository: GIFRepository

    @Inject
    lateinit var backupManager: BackupManager

    private lateinit var serviceConnection: TariWalletServiceConnection
    private val walletService: TariWalletService?
        get() = serviceConnection.currentState.service

    private var requiredConfirmationCount: Long = 0

    private val cancelledTxs = CopyOnWriteArrayList<CancelledTx>()
    private val completedTxs = CopyOnWriteArrayList<CompletedTx>()
    private val pendingInboundTxs = CopyOnWriteArrayList<PendingInboundTx>()
    private val pendingOutboundTxs = CopyOnWriteArrayList<PendingOutboundTx>()

    private val handler: Handler = Handler(Looper.getMainLooper())

    // This listener is used only to animate the visibility of the scroll depth gradient view.
    private val recyclerViewScrollListener = RecyclerViewScrollListener(::onRecyclerViewScrolled)
    private val shakeDetector = ShakeDetector(this)
    private lateinit var ui: FragmentTxListBinding
    private lateinit var recyclerViewAdapter: TxListAdapter
    private lateinit var balanceInfo: BalanceInfo
    private lateinit var balanceViewController: BalanceViewController
    private lateinit var updateProgressViewController: UpdateProgressViewController
    private var currentDialog: Dialog? = null
    private val container = ResourceContainer()
    private var isOnboarding = false
    private var isInDraggingSession = false
    private var testnetTariRequestIsInProgress = false

    // TODO(nyarian): remove
    private var sendTariButtonIsVisible = true

    // TODO(nyarian): remove
    private var testnetTariRequestIsWaitingOnConnection = false

    private var txEventUIActionDelayMs = 500L
    private var txReceivedDelayedAction: Disposable? = null
    private var txReplyReceivedDelayedAction: Disposable? = null
    private var txFinalizedDelayedAction: Disposable? = null
    private var txMinedUnconfirmedDelayedAction: Disposable? = null
    private var txMinedDelayedAction: Disposable? = null
    private var txCancelledDelayedAction: Disposable? = null

    private val txListIsEmpty: Boolean
        get() = cancelledTxs.isEmpty()
                && completedTxs.isEmpty()
                && pendingInboundTxs.isEmpty()
                && pendingOutboundTxs.isEmpty()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentTxListBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) tracker.screen("/home", "Home - Transaction List")
        bindToWalletService()
        setupUI()
        subscribeToEventBus()
    }

    override fun onStart() {
        startShakeDetector()
        super.onStart()
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        shakeDetector.stop()
        super.onStop()
    }

    override fun onDestroyView() {
        EventBus.unsubscribe(this)
        EventBus.unsubscribeFromNetworkConnectionState(this)
        if (::updateProgressViewController.isInitialized) {
            updateProgressViewController.destroy()
        }
        container.dispose()
        super.onDestroyView()
    }

    // Shaking the device should take the user to the debug screen
    private fun startShakeDetector() {
        (requireContext().getSystemService(AppCompatActivity.SENSOR_SERVICE) as? SensorManager)
            ?.let(shakeDetector::start)
    }

    // region initial setup (UI and else)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        ui.txListHeaderView.setTopMargin(-dimenPx(R.dimen.common_header_height))
        updateProgressViewController =
            UpdateProgressViewController(
                ui.updateProgressContentView,
                this
            )
        ui.scrollView.bindUI()
        ui.scrollView.listenerWeakReference = WeakReference(this)
        ui.scrollView.updateProgressViewController = updateProgressViewController
        setupRecyclerView()
        ui.scrollBgEnablerView.setOnTouchListener(this)
        ui.scrollView.setOnTouchListener(this)
        ui.txRecyclerView.setOnTouchListener(this)

        ui.apply {
            headerElevationView.alpha = 0F
            availableBalanceTextView.alpha = 0F
            walletInfoImageView.alpha = 0F
            balanceGemImageView.alpha = 0F
            scrollContentView.alpha = 0F
            noTxsInfoTextView.gone()
            onboardingContentView.gone()
            topContentContainerView.invisible()
        }
        setupCTAs()
    }

    private fun setupCTAs() {
        ui.scrollView.post { setupScrollView() }
        ui.closeTxListButton.setOnClickListener(::minimizeListButtonClicked)
        ui.grabberContainerView.setOnClickListener(::grabberContainerViewClicked)
        ui.grabberContainerView.setOnLongClickListener {
            grabberContainerViewLongClicked()
            true
        }
    }

    private fun setupRecyclerView() {
        ui.txRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter =
            TxListAdapter(
                cancelledTxs,
                completedTxs,
                pendingInboundTxs,
                pendingOutboundTxs,
                { requiredConfirmationCount },
                repository,
                Glide.with(this),
            ) {
                (requireActivity() as TxListRouter).toTxDetails(it)
            }
        ui.txRecyclerView.adapter = recyclerViewAdapter
        // hide vertical scrollbar initially
        ui.txRecyclerView.isVerticalScrollBarEnabled = false
    }

    private fun setupScrollView() {
        val recyclerViewHeight = ui.rootView.height - dimenPx(R.dimen.common_header_height)
        val contentHeight =
            dimenPx(R.dimen.home_tx_list_container_minimized_top_margin) +
                    dimenPx(R.dimen.home_grabber_container_height) + recyclerViewHeight
        ui.recyclerViewContainerView.setLayoutHeight(recyclerViewHeight)
        ui.scrollContentView.setLayoutHeight(contentHeight)
        ui.scrollView.recyclerViewContainerInitialHeight = recyclerViewHeight
        ui.scrollView.scrollToTop()

        ui.scrollView.setOnScrollChangeListener(this)
        ui.txRecyclerView.setOnScrollChangeListener(this)
        ui.txRecyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.App.AppForegrounded>(this) {
            if (walletService != null
                && updateProgressViewController.state == UpdateProgressViewController.State.IDLE
            ) {
                lifecycleScope.launch(Dispatchers.Main) {
                    updateProgressViewController.reset()
                    updateProgressViewController.start(walletService!!)
                    ui.scrollView.beginUpdate()
                }
            }
        }

        EventBus.subscribe<Event.Wallet.TxReceived>(this) {
            if (updateProgressViewController.state != UpdateProgressViewController.State.RECEIVING) {
                onTxReceived(it.tx)
            }
        }
        EventBus.subscribe<Event.Wallet.TxReplyReceived>(this) {
            onTxReplyReceived(it.tx)
        }
        EventBus.subscribe<Event.Wallet.TxFinalized>(this) {
            onTxFinalized(it.tx)
        }
        EventBus.subscribe<Event.Wallet.InboundTxBroadcast>(this) {
            onInboundTxBroadcast(it.tx)
        }
        EventBus.subscribe<Event.Wallet.OutboundTxBroadcast>(this) {
            onOutboundTxBroadcast(it.tx)
        }
        EventBus.subscribe<Event.Wallet.TxMinedUnconfirmed>(this) {
            onTxMinedUnconfirmed(it.tx)
        }
        EventBus.subscribe<Event.Wallet.TxMined>(this) {
            onTxMined(it.tx)
        }
        EventBus.subscribe<Event.Wallet.TxCancelled>(this) {
            if (updateProgressViewController.state != UpdateProgressViewController.State.RECEIVING) {
                onTxCancelled(it.tx)
            }
        }

        EventBus.subscribe<Event.Testnet.TestnetTariRequestSuccessful>(this) {
            lifecycleScope.launch(Dispatchers.Main) { testnetTariRequestSuccessful() }
        }
        EventBus.subscribe<Event.Testnet.TestnetTariRequestError>(this) { event ->
            lifecycleScope.launch(Dispatchers.Main) { testnetTariRequestError(event.errorMessage) }
        }

        EventBus.subscribe<Event.Tx.TxSendSuccessful>(this) {
            lifecycleScope.launch(Dispatchers.Main) { onTxSendSuccessful(it.txId) }
        }
        EventBus.subscribe<Event.Tx.TxSendFailed>(this) { event ->
            lifecycleScope.launch(Dispatchers.Main) { onTxSendFailed(event.failureReason) }
        }

        EventBus.subscribeToNetworkConnectionState(this) { networkConnectionState ->
            if (testnetTariRequestIsWaitingOnConnection
                && networkConnectionState == NetworkConnectionState.CONNECTED
            ) {
                requestTestnetTari()
            }
        }

        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) {
            onContactAddedOrUpdated(it.contactPublicKey, it.contactAlias)
        }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) {
            onContactRemoved(it.contactPublicKey)
        }
    }

    // endregion

    private fun onTxReceived(tx: PendingInboundTx) {
        pendingInboundTxs.add(tx)
        txReceivedDelayedAction?.dispose()
        // update balance
        txReceivedDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    updateBalanceInfoData()
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateBalanceInfoUI(restart = false)
                        showWalletBackupPromptIfNecessary()
                        updateTxListUI()
                    }
                }
    }

    private fun showWalletBackupPromptIfNecessary() {
        if (context != null && (!sharedPrefsWrapper.backupIsEnabled || sharedPrefsWrapper.backupPassword == null)) {
            val inboundTransactionsCount = pendingInboundTxs.size + completedTxs.asSequence()
                .filter { it.direction == Tx.Direction.INBOUND }.count()
            val tarisAmount =
                balanceInfo.availableBalance.tariValue + balanceInfo.pendingIncomingBalance.tariValue
            when {
                inboundTransactionsCount >= 5
                        && tarisAmount >= BigDecimal("25000")
                        && sharedPrefsWrapper.backupIsEnabled
                        && sharedPrefsWrapper.backupPassword == null -> showSecureYourBackupsDialog()
                inboundTransactionsCount >= 4
                        && tarisAmount >= BigDecimal("8000")
                        && !sharedPrefsWrapper.backupIsEnabled -> showRepeatedBackUpPrompt()
                // Non-faucet transactions only here. Calculation is performed here to avoid
                // unnecessary calculations as previous two cases have much greater chance to happen
                pendingInboundTxs.size + completedTxs
                    .filter { it.direction == Tx.Direction.INBOUND }
                    .filterNot { it.status == TxStatus.IMPORTED }
                    .count() >= 1
                        && !sharedPrefsWrapper.backupIsEnabled -> showInitialBackupPrompt()
            }
        }
    }

    private fun showInitialBackupPrompt() {
        BackupWalletPrompt(
            context = requireContext(),
            regularTitlePart = string(R.string.home_back_up_wallet_initial_title_regular_part),
            highlightedTitlePart = string(R.string.home_back_up_wallet_initial_title_highlighted_part),
            description = string(R.string.home_back_up_wallet_initial_description),
            router = requireContext() as TxListRouter
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun showRepeatedBackUpPrompt() {
        BackupWalletPrompt(
            context = requireContext(),
            regularTitlePart = string(R.string.home_back_up_wallet_repeated_title_regular_part),
            highlightedTitlePart = string(R.string.home_back_up_wallet_repeated_title_highlighted_part),
            description = string(R.string.home_back_up_wallet_repeated_description),
            router = requireContext() as TxListRouter
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun showSecureYourBackupsDialog() {
        BackupWalletPrompt(
            context = requireContext(),
            title = string(R.string.home_back_up_wallet_encrypt_title),
            description = string(R.string.home_back_up_wallet_encrypt_description),
            ctaText = string(R.string.home_back_up_wallet_encrypt_cta),
            dismissText = string(R.string.home_back_up_wallet_delay_encrypt_cta),
            router = requireContext() as TxListRouter
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun replaceDialog(dialog: Dialog) {
        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        txReplyReceivedDelayedAction?.dispose()
        txReplyReceivedDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    updateBalanceInfoData()
                    lifecycleScope.launch(Dispatchers.Main) {
                        lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
                    }
                }
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        txFinalizedDelayedAction?.dispose()
        txFinalizedDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    updateBalanceInfoData()
                    lifecycleScope.launch(Dispatchers.Main) {
                        lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
                    }
                }
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        // just update data - no UI change required
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        // just update data - no UI change required
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onTxMinedUnconfirmed(tx: CompletedTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        txMinedUnconfirmedDelayedAction?.dispose()
        txMinedUnconfirmedDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
                }
    }

    private fun onTxMined(tx: CompletedTx) {
        pendingInboundTxs.removeIf { it.id == tx.id }
        pendingOutboundTxs.removeIf { it.id == tx.id }

        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        txMinedDelayedAction?.dispose()
        txMinedDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
                }
    }

    private fun onTxCancelled(tx: CancelledTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        cancelledTxs.add(tx)
        txCancelledDelayedAction?.dispose()
        txCancelledDelayedAction =
            Observable
                .timer(txEventUIActionDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    updateBalanceInfoData()
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateBalanceInfoUI(restart = false)
                        updateTxListUI()
                    }
                }
    }

    private fun onContactAddedOrUpdated(publicKey: PublicKey, alias: String) {
        val contact = Contact(publicKey, alias)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.publicKey == publicKey }
            .forEach { it.user = contact }
        lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
    }

    private fun onContactRemoved(publicKey: PublicKey) {
        val user = User(publicKey)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.publicKey == publicKey }
            .forEach { it.user = user }
        lifecycleScope.launch(Dispatchers.Main) { updateTxListUI() }
    }

    // region service connection

    private fun bindToWalletService() {
        serviceConnection = ViewModelProvider(requireActivity())
            .get(TariWalletServiceConnection::class.java)
        serviceConnection.connection.observe(viewLifecycleOwner, {
            if (it.status == CONNECTED) onServiceConnected()
        })
    }

    private fun onServiceConnected() {
        lifecycleScope.launch(Dispatchers.IO) {
            updateTxListData()
            updateBalanceInfoData()
            fetchRequiredConfirmationCount()
            withContext(Dispatchers.Main) {
                if (activity != null) {
                    // TODO(nyarian): properly handle memory leak
                    // Means that service was connected when the fragment's is already destroyed
                    initializeTxListUI()
                }
            }
        }
    }

    // endregion

    /**
     * Fetches transactions from the service & updates the lists.
     */
    private fun updateTxListData() {
        val error = WalletError()
        val service = walletService!!
        val walletCancelledTxs = service.getCancelledTxs(error)
        val walletCompletedTxs = service.getCompletedTxs(error)
        val walletPendingInboundTxs = service.getPendingInboundTxs(error)
        val walletPendingOutboundTxs = service.getPendingOutboundTxs(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        cancelledTxs.repopulate(walletCancelledTxs)
        completedTxs.repopulate(walletCompletedTxs)
        pendingInboundTxs.repopulate(walletPendingInboundTxs)
        pendingOutboundTxs.repopulate(walletPendingOutboundTxs)
    }

    private fun initializeTxListUI() {
        if (sharedPrefsWrapper.onboardingDisplayedAtHome) {
            recyclerViewAdapter.notifyDataChanged()
            playNonOnboardingStartupAnim()
            if (txListIsEmpty) {
                showNoTxsTextView()
            }
            if (!sharedPrefsWrapper.faucetTestnetTariRequestCompleted
                && !sharedPrefsWrapper.isRestoredWallet
            ) {
                requestTestnetTari()
            }
            updateProgressViewController.reset()
            ui.scrollView.beginUpdate()
            updateProgressViewController.start(walletService!!)
        } else {
            isOnboarding = true
            playOnboardingAnim()
            sharedPrefsWrapper.onboardingDisplayedAtHome = true
        }
    }

    private fun updateTxListUI() {
        recyclerViewAdapter.notifyDataChanged()
        if (txListIsEmpty) {
            showNoTxsTextView()
        } else {
            ui.noTxsInfoTextView.gone()
        }
    }

    private fun updateBalanceInfoData(): Boolean {
        val error = WalletError()
        // get balance
        balanceInfo = walletService!!.getBalanceInfo(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        return true
    }

    private fun fetchRequiredConfirmationCount() {
        val error = WalletError()
        val service = walletService!!
        requiredConfirmationCount = service.getRequiredConfirmationCount(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun updateBalanceInfoUI(restart: Boolean) {
        if (restart) {
            ui.balanceDigitContainerView.removeAllViews()
            ui.balanceDecimalsDigitContainerView.removeAllViews()
            balanceViewController =
                BalanceViewController(
                    requireContext(),
                    ui.balanceDigitContainerView,
                    ui.balanceDecimalsDigitContainerView,
                    balanceInfo
                )
            // show digits
            balanceViewController.runStartupAnimation()
        } else {
            if (::balanceViewController.isInitialized) {
                balanceViewController.balanceInfo = balanceInfo
            }
        }
    }

    /**
     * The startup animation - reveals the list, balance and other views.
     */
    private fun playNonOnboardingStartupAnim() {
        ui.topContentContainerView.visible()
        updateBalanceInfoUI(true)
        ui.scrollView.scrollToTop()
        sendTariButtonIsVisible = true
        handler.postDelayed(Constants.UI.Home.startupAnimDurationMs) { ui.blockerView.gone() }
        ValueAnimator.ofFloat(0.0F, 1.0F).apply {
            duration = Constants.UI.Home.startupAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                // animate the list (will move upwards)
                ui.scrollContentView.y =
                    dimenPx(R.dimen.home_scroll_view_startup_anim_height) * (1 - value)
                ui.scrollContentView.alpha = value
                // reveal balance title, QR code button and balance gem image
                ui.availableBalanceTextView.alpha = value
                ui.walletInfoImageView.alpha = value
                ui.balanceGemImageView.alpha = value
            }
        }
            .also { AnimationResource(it).attachAndCutoffOnFinish(container) }
            .start()
    }

    /**
     * Play onboarding animation.
     */
    private fun playOnboardingAnim() {
        ui.onboardingContentView.visible()
        updateBalanceInfoData()
        // initialize the balance view controller
        balanceViewController =
            BalanceViewController(
                requireContext(),
                ui.balanceDigitContainerView,
                ui.balanceDecimalsDigitContainerView,
                balanceInfo // initial value
            )

        ui.scrollView.scrollTo(0, ui.scrollView.height)
        ui.scrollContentView.alpha = 1F
        // scroll view translation animation
        val scrollViewTransAnim =
            ObjectAnimator.ofFloat(
                ui.scrollView,
                View.TRANSLATION_Y,
                ui.scrollView.height.toFloat(),
                dimenPx(R.dimen.home_main_content_top_margin).toFloat()
            )
        scrollViewTransAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
        // background fade animation
        val blackBgViewFadeAnim = ValueAnimator.ofFloat(0F, 1F)
        blackBgViewFadeAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.txListOverlayView.alpha = value
        }
        // the animation set
        val animSet = AnimatorSet()
        animSet.playTogether(scrollViewTransAnim, blackBgViewFadeAnim)
        animSet.duration = Constants.UI.Home.welcomeAnimationDurationMs
        animSet.addListener(
            onEnd = {
                ui.topContentContainerView.visible()
                ui.availableBalanceTextView.alpha = 1F
                ui.walletInfoImageView.alpha = 1F
                ui.balanceGemImageView.alpha = 1F
            }
        )
        animSet.start()

        // show interstitial & finish after delay
        ui.scrollView.isScrollable = false
        handler.postDelayed(ONBOARDING_INTERSTITIAL_TIME) {
            ui.scrollView.smoothScrollTo(0, 0)
            ui.blockerView.gone()
        }

    }

    private fun showNoTxsTextView() {
        ui.noTxsInfoTextView.alpha = 0F
        ui.noTxsInfoTextView.visible()
        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = Constants.UI.mediumDurationMs
            addUpdateListener { ui.noTxsInfoTextView.alpha = it.animatedValue as Float }
        }.start()
    }

    private fun requestTestnetTari() {
        if (testnetTariRequestIsInProgress) return
        if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
            testnetTariRequestIsWaitingOnConnection = true
        } else {
            testnetTariRequestIsWaitingOnConnection = false
            testnetTariRequestIsInProgress = true
            lifecycleScope.launch(Dispatchers.IO) {
                val error = WalletError()
                walletService!!.requestTestnetTari(error)
                if (error.code != WalletErrorCode.NO_ERROR) {
                    TODO("Unhandled wallet error: ${error.code}")
                }
            }
        }
    }

    private fun testnetTariRequestSuccessful() {
        lifecycleScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val importedTx = walletService!!.importTestnetUTXO(
                string(R.string.first_testnet_utxo_tx_message),
                error
            )
            if (error.code != WalletErrorCode.NO_ERROR) {
                TODO("Unhandled wallet error: ${error.code}")
            }
            sharedPrefsWrapper.faucetTestnetTariRequestCompleted = true
            sharedPrefsWrapper.firstTestnetUTXOTxId = importedTx.id
            completedTxs.add(importedTx)
            updateBalanceInfoData()
            // update UI
            lifecycleScope.launch(Dispatchers.Main) {
                updateTxListUI()
                updateBalanceInfoUI(restart = false)
            }
            // display dialog
            handler.postDelayed(Constants.UI.Home.showTariBotDialogDelayMs) {
                if (context != null) showTestnetTariReceivedDialog(importedTx.user.publicKey)
            }
            testnetTariRequestIsInProgress = false
        }
    }

    private fun testnetTariRequestError(errorMessage: String) {
        testnetTariRequestIsInProgress = false
        if (context != null) {
            val title = string(R.string.faucet_error_title)
            var description = errorMessage
            if (errorMessage.contains("many allocation attempts")) {
                description = string(R.string.faucet_error_too_many_allocation_attemps)
            }
            ErrorDialog(
                context ?: return,
                title = title,
                description = description
            ).show()
        }
    }

    private fun showTestnetTariReceivedDialog(testnetSenderPublicKey: PublicKey) {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = requireContext(),
            layoutId = R.layout.home_dialog_testnet_tari_received,
            canceledOnTouchOutside = false
        ).apply {
            findViewById<TextView>(R.id.home_testnet_tari_received_dlg_txt_title).text =
                string(R.string.home_tari_bot_you_got_tari_dlg_title).applyFontStyle(
                    requireContext(),
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    listOf(string(R.string.home_tari_bot_you_got_tari_dlg_title_bold_part)),
                    CustomFont.AVENIR_LT_STD_BLACK
                )
            findViewById<TextView>(R.id.home_tari_bot_dialog_txt_try_later)
                .setOnClickListener {
                    currentDialog = null
                    dismiss()
                }
            findViewById<TextView>(R.id.home_tari_bot_dialog_btn_send_tari)
                .setOnClickListener {
                    currentDialog = null
                    dismiss()
                    sendTariToUser(testnetSenderPublicKey)
                }
        }.apply { show() }
            .asAndroidDialog()
    }

    private fun showTTLStoreDialog() {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = requireContext(),
            layoutId = R.layout.home_dialog_ttl_store
        ).apply {
            findViewById<TextView>(R.id.home_ttl_store_dialog_txt_title).text =
                string(R.string.home_ttl_store_dlg_title).applyFontStyle(
                    requireContext(),
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    listOf(string(R.string.home_ttl_store_dlg_title_bold_part)),
                    CustomFont.AVENIR_LT_STD_BLACK
                )
            findViewById<View>(R.id.home_ttl_store_dialog_btn_later)
                .setOnClickListener {
                    currentDialog = null
                    dismiss()
                }
            findViewById<View>(R.id.home_ttl_store_dialog_vw_store_button)
                .setOnClickListener {
                    it.setOnClickListener(null)
                    visitTTLStore()
                    handler.postDelayed(Constants.UI.mediumDurationMs) {
                        currentDialog = null
                        dismiss()
                    }
                }
            findViewById<View>(R.id.home_ttl_store_dialog_vw_top_spacer)
                .setOnClickListener { dismiss() }
        }.apply { show() }
            .asAndroidDialog()
    }

    private fun visitTTLStore() {
        (requireContext() as TxListRouter).toTTLStore()
    }

    private fun sendTariToUser(
        recipientPublicKey: PublicKey,
        parameters: Map<String, String> = emptyMap()
    ) {
        // get contact or just user
        val error = WalletError()
        val contacts = walletService!!.getContacts(error)
        val recipientUser = when (error.code) {
            WalletErrorCode.NO_ERROR -> contacts
                .firstOrNull { it.publicKey == recipientPublicKey } ?: User(recipientPublicKey)
            else -> User(recipientPublicKey)
        }
        // prepare intent
        val intent = Intent(requireContext(), SendTariActivity::class.java)
        intent.putExtra("recipientUser", recipientUser)
        parameters[DeepLink.PARAMETER_NOTE]?.let { intent.putExtra(DeepLink.PARAMETER_NOTE, it) }
        parameters[DeepLink.PARAMETER_AMOUNT]?.toDoubleOrNull()
            ?.let { intent.putExtra(DeepLink.PARAMETER_AMOUNT, it) }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun endOnboarding() {
        isOnboarding = false
        ui.onboardingContentView.gone()
        ui.txListHeaderView.visible()
        lifecycleScope.launch(Dispatchers.IO) {
            updateTxListData()
            if (isActive) {
                updateBalanceInfoData()
                lifecycleScope.launch(Dispatchers.Main) {
                    updateBalanceInfoUI(restart = false)
                    updateTxListUI()
                }
            }
        }
        // request Testnet Tari if no txs
        if (txListIsEmpty) {
            handler.postDelayed(Constants.UI.xxLongDurationMs, action = this::requestTestnetTari)
        }
    }

    /**
     * Called when an outgoing transaction has been successful.
     */
    private fun onTxSendSuccessful(txId: TxId) {
        ui.scrollView.scrollToTop()
        ui.txRecyclerView.scrollToPosition(0)
        val topMargin =
            ui.rootView.height - dimenPx(R.dimen.home_tx_list_container_minimized_top_margin)
        ui.scrollContentView.y = topMargin.toFloat()
        val anim = ValueAnimator.ofInt(
            topMargin,
            0
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Int
            ui.scrollContentView.y = value.toFloat()
        }
        anim.duration = Constants.UI.longDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()

        // update data
        lifecycleScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val tx = walletService?.getPendingOutboundTxById(txId, error)
            if (error.code != WalletErrorCode.NO_ERROR) {
                TODO("Unhandled wallet error: ${error.code}")
            }
            pendingOutboundTxs.add(tx)
            updateBalanceInfoData()
            lifecycleScope.launch(Dispatchers.Main) {
                updateBalanceInfoUI(restart = true)
                updateTxListUI()
            }
        }

        // import second testnet UTXO if it hasn't been imported yet
        if (sharedPrefsWrapper.testnetTariUTXOKeyList.isNotEmpty()) {
            handler.postDelayed(SECOND_UTXO_IMPORT_DELAY, action = this::importSecondUTXO)
        }
    }

    /**
     * Called when an outgoing transaction has failed.
     */
    private fun onTxSendFailed(failureReason: FinalizeSendTxFragment.FailureReason) {
        when (failureReason) {
            FinalizeSendTxFragment.FailureReason.NETWORK_CONNECTION_ERROR -> {
                displayNetworkConnectionErrorDialog()
            }
            FinalizeSendTxFragment.FailureReason.BASE_NODE_CONNECTION_ERROR, FinalizeSendTxFragment.FailureReason.SEND_ERROR -> {
                displayBaseNodeConnectionErrorDialog()
            }
        }
    }

    private fun importSecondUTXO() {
        lifecycleScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val importedTx = walletService!!.importTestnetUTXO(
                string(R.string.second_testnet_utxo_tx_message),
                error
            )
            if (error.code != WalletErrorCode.NO_ERROR) {
                TODO("Unhandled wallet error: ${error.code}")
            }
            sharedPrefsWrapper.secondTestnetUTXOTxId = importedTx.id
            completedTxs.add(importedTx)
            updateBalanceInfoData()
            // update UI
            lifecycleScope.launch(Dispatchers.Main) {
                updateTxListUI()
                updateBalanceInfoUI(restart = false)
            }
            // display store dialog
            handler.postDelayed(SECOND_UTXO_STORE_OPEN_DELAY) {
                if (context != null) showTTLStoreDialog()
            }
        }
    }

    private fun minimizeListButtonClicked(view: View) {
        view.temporarilyDisableClick()
        ui.scrollView.smoothScrollTo(0, 0)
        ui.txRecyclerView.smoothScrollToPosition(0)
    }

    private fun grabberContainerViewClicked(view: View) {
        view.temporarilyDisableClick()
        ui.scrollView.smoothScrollTo(
            0,
            ui.scrollContentView.height - ui.scrollView.height
        )
    }

    /**
     * A shake will take the user to the debug screen.
     */
    override fun hearShake() {
        val intent = Intent(requireContext(), DebugActivity::class.java)
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun grabberContainerViewLongClicked() {
        val intent = Intent(requireContext(), DebugActivity::class.java)
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onSwipeRefresh(source: CustomScrollView) {
        updateProgressViewController.start(walletService!!)
    }

    override fun updateHasFailed(
        source: UpdateProgressViewController,
        failureReason: UpdateProgressViewController.FailureReason,
        validationResult: BaseNodeValidationResult?
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            ui.scrollView.finishUpdate()
            if (validationResult != BaseNodeValidationResult.ABORTED) {
                when (failureReason) {
                    UpdateProgressViewController.FailureReason.NETWORK_CONNECTION_ERROR -> {
                        displayNetworkConnectionErrorDialog()
                    }
                    UpdateProgressViewController.FailureReason.BASE_NODE_VALIDATION_ERROR -> {
                        displayBaseNodeConnectionErrorDialog()
                    }
                }
            }
        }
    }

    override fun updateHasCompleted(
        source: UpdateProgressViewController,
        receivedTxCount: Int,
        cancelledTxCount: Int
    ) {
        lifecycleScope.launch(Dispatchers.Main) { ui.scrollView.finishUpdate() }

        lifecycleScope.launch(Dispatchers.IO) {
            updateTxListData()
            updateBalanceInfoData()
            withContext(Dispatchers.Main) {
                updateBalanceInfoUI(restart = false)
                updateTxListUI()
                if (receivedTxCount > 0) {
                    showWalletBackupPromptIfNecessary()
                }
            }
        }
    }

    private fun displayNetworkConnectionErrorDialog() {
        if (context != null) {
            currentDialog?.dismiss()
            currentDialog = BottomSlideDialog(
                context = requireContext(),
                layoutId = R.layout.tx_failed_dialog,
                dismissViewId = R.id.tx_failed_dialog_txt_close
            ).apply {
                val titleTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_title)
                titleTextView.text = string(R.string.error_no_connection_title)
                val descriptionTextView =
                    findViewById<TextView>(R.id.tx_failed_dialog_txt_description)
                descriptionTextView.text = string(R.string.error_no_connection_description)
                // set text
            }.apply { show() }
                .asAndroidDialog()
                .apply { setOnDismissListener { currentDialog = null } }
        }
    }

    private fun displayBaseNodeConnectionErrorDialog() {
        if (context != null) {
            currentDialog?.dismiss()
            currentDialog = BottomSlideDialog(
                context = requireContext(),
                layoutId = R.layout.tx_failed_dialog,
                dismissViewId = R.id.tx_failed_dialog_txt_close
            ).apply {
                val titleTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_title)
                titleTextView.text = string(R.string.error_node_unreachable_title)
                val descriptionTextView =
                    findViewById<TextView>(R.id.tx_failed_dialog_txt_description)
                descriptionTextView.text = string(R.string.error_node_unreachable_description)
                // set text
            }.apply { show() }
                .asAndroidDialog()
                .apply { setOnDismissListener { currentDialog = null } }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view != null && event != null && (view === ui.scrollView || view === ui.txRecyclerView)) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> isInDraggingSession = true
                MotionEvent.ACTION_UP -> {
                    if (ui.scrollView.scrollY == 0 && !ui.txRecyclerView.isScrolledToTop()) {
                        ui.txRecyclerView.smoothScrollToPosition(0)
                        handler.postDelayed(Constants.UI.mediumDurationMs) {
                            recyclerViewScrollListener.reset()
                            ui.headerElevationView.alpha = 0F
                        }
                    }
                    ui.scrollView.flingIsRunning = false
                    handler.postDelayed(50L, action = ui.scrollView::completeScroll)
                    isInDraggingSession = false
                }
            }
        }
        return false
    }


    /**
     * Scroll-related UI changes.
     */
    override fun onScrollChange(
        view: View?,
        scrollX: Int,
        scrollY: Int,
        oldScrollX: Int,
        oldScrollY: Int
    ) {
        if (view is CustomScrollView) {
            ui.txRecyclerView.isVerticalScrollBarEnabled = (view.scrollY == view.maxScrollY)
            val ratio = ui.scrollView.scrollY.toFloat() / ui.scrollView.maxScrollY.toFloat()
            ui.onboardingContentView.alpha = ratio
            ui.txListOverlayView.alpha = ratio
            val topContentMarginTopExtra =
                (ratio * dimenPx(R.dimen.home_top_content_container_scroll_vertical_shift)).toInt()
            ui.topContentContainerView.setTopMargin(
                dimenPx(R.dimen.home_top_content_container_view_top_margin)
                        + topContentMarginTopExtra
            )
            ui.txListTitleTextView.alpha = ratio
            ui.closeTxListButton.alpha = ratio

            if (!isOnboarding) {
                ui.txListHeaderView.setTopMargin(
                    ((ratio - 1) * dimenPx(R.dimen.common_header_height)).toInt()
                )
                ui.grabberView.alpha = max(
                    0F,
                    1F - ratio * GRABBER_ALPHA_SCROLL_COEFFICIENT
                )
                val width = (max(0F, 1F - ratio * GRABBER_WIDTH_SCROLL_COEFFICIENT)
                        * dimenPx(R.dimen.home_grabber_width)).toInt()
                ui.grabberView.setLayoutWidth(width)
                val grabberBgDrawable = ui.grabberContainerView.background as GradientDrawable
                grabberBgDrawable.cornerRadius = max(
                    0F,
                    1F - ratio * GRABBER_CORNER_RADIUS_SCROLL_COEFFICIENT
                ) * dimenPx(R.dimen.home_grabber_corner_radius)

                if (ratio == 0F && !isInDraggingSession) {
                    if (!ui.txRecyclerView.isScrolledToTop()) {
                        ui.txRecyclerView.smoothScrollToPosition(0)
                    }
                    handler.postDelayed(Constants.UI.mediumDurationMs) {
                        recyclerViewScrollListener.reset()
                        ui.headerElevationView.alpha = 0F
                    }
                }
            } else if (ratio == 0F && !isInDraggingSession) { // is onboarding
                ui.scrollView.isScrollable = true
                endOnboarding()
            }

            ui.giftCtaView.setTopMargin(
                dimenPx(R.dimen.home_wallet_info_button_initial_top_margin) + ui.scrollView.scrollY + topContentMarginTopExtra
            )
        }
    }

    // region scroll depth gradient view controls

    private fun onRecyclerViewScrolled(totalDeltaY: Int) {
        ui.headerElevationView.alpha = min(
            Constants.UI.scrollDepthShadowViewMaxOpacity,
            totalDeltaY / dimenPx(R.dimen.home_tx_list_item_height).toFloat()
        )
    }

    class RecyclerViewScrollListener(private val onScroll: (Int) -> Unit) :
        RecyclerView.OnScrollListener() {

        private var totalDeltaY = 0

        fun reset() {
            totalDeltaY = 0
        }

        override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
            super.onScrolled(recyclerView, dX, dY)
            totalDeltaY += dY
            onScroll(totalDeltaY)
        }

    }

    // endregion

    class BackupWalletPrompt private constructor(
        private val dialog: Dialog
    ) {

        constructor(
            context: Context,
            title: CharSequence,
            description: CharSequence,
            ctaText: CharSequence = context.string(R.string.home_back_up_wallet_back_up_cta),
            dismissText: CharSequence = context.string(R.string.home_back_up_wallet_delay_back_up_cta),
            router: TxListRouter
        ) : this(
            BottomSlideDialog(
                context,
                R.layout.dialog_backup_wallet_prompt,
                canceledOnTouchOutside = false,
                dismissViewId = R.id.home_backup_wallet_prompt_dismiss_cta_view
            ).apply {
                findViewById<TextView>(R.id.home_backup_wallet_prompt_title_text_view).text = title
                findViewById<TextView>(R.id.home_backup_wallet_prompt_description_text_view).text =
                    description
                findViewById<TextView>(R.id.home_backup_wallet_prompt_dismiss_cta_view).text =
                    dismissText
                val backupCta =
                    findViewById<TextView>(R.id.home_backup_wallet_prompt_backup_cta_view)
                backupCta.text = ctaText
                backupCta.setOnClickListener {
                    router.toAllSettings()
                    dismiss()
                }
            }.asAndroidDialog()
        )

        constructor(
            context: Context,
            regularTitlePart: CharSequence,
            highlightedTitlePart: CharSequence,
            description: CharSequence,
            ctaText: CharSequence = context.string(R.string.home_back_up_wallet_back_up_cta),
            dismissText: CharSequence = context.string(R.string.home_back_up_wallet_delay_back_up_cta),
            router: TxListRouter
        ) : this(
            context,
            SpannableStringBuilder().apply {
                val highlightedPart = SpannableString(highlightedTitlePart)
                highlightedPart.setSpan(
                    CustomTypefaceSpan("", CustomFont.AVENIR_LT_STD_HEAVY.asTypeface(context)),
                    0,
                    highlightedPart.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                insert(0, regularTitlePart)
                insert(regularTitlePart.length, " ")
                insert(regularTitlePart.length + 1, highlightedPart)
            },
            description,
            ctaText,
            dismissText,
            router
        )

        fun asAndroidDialog() = dialog

    }

    interface TxListRouter {
        fun toTxDetails(tx: Tx)

        fun toTTLStore()

        fun toAllSettings()
    }

    companion object {
        private const val GRABBER_ALPHA_SCROLL_COEFFICIENT = 1.2F
        private const val GRABBER_WIDTH_SCROLL_COEFFICIENT = 1.1F
        private const val GRABBER_CORNER_RADIUS_SCROLL_COEFFICIENT = 1.4F
        private const val ONBOARDING_INTERSTITIAL_TIME = 4500L
        private const val SECOND_UTXO_IMPORT_DELAY = 2500L
        private const val SECOND_UTXO_STORE_OPEN_DELAY = 3000L
    }

}
