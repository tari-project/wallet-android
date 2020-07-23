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
package com.tari.android.wallet.ui.activity.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.GradientDrawable
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.ActivityHomeBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.debug.DebugActivity
import com.tari.android.wallet.ui.activity.home.adapter.TxListAdapter
import com.tari.android.wallet.ui.activity.profile.WalletInfoActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.activity.settings.SettingsActivity
import com.tari.android.wallet.ui.activity.tx.TxDetailActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.CustomTypefaceSpan
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.FinalizeSendTxFragment
import com.tari.android.wallet.ui.fragment.send.FinalizeSendTxFragment.FailureReason.*
import com.tari.android.wallet.ui.fragment.store.StoreDialogFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Home activity - transaction list.
 *
 * @author The Tari Development Team
 */
internal class HomeActivity : AppCompatActivity(),
    ServiceConnection,
    View.OnScrollChangeListener,
    View.OnTouchListener,
    Animation.AnimationListener,
    CustomScrollView.Listener,
    UpdateProgressViewController.Listener,
    ShakeDetector.Listener {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var backupManager: BackupManager

    private lateinit var ui: ActivityHomeBinding

    // tx list
    private lateinit var recyclerViewAdapter: TxListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private val cancelledTxs = CopyOnWriteArrayList<CancelledTx>()
    private val completedTxs = CopyOnWriteArrayList<CompletedTx>()
    private val pendingInboundTxs = CopyOnWriteArrayList<PendingInboundTx>()
    private val pendingOutboundTxs = CopyOnWriteArrayList<PendingOutboundTx>()

    // balance
    private lateinit var balanceInfo: BalanceInfo

    // balance controller
    private lateinit var balanceViewController: BalanceViewController

    // grabber drag animation constants
    private var grabberViewAlphaScrollAnimCoefficient = 1.2f
    private var grabberViewWidthScrollAnimCoefficient = 1.1f
    private var grabberViewCornerRadiusScrollAnimCoefficient = 1.4f

    private var isOnboarding = false
    private var testnetTariRequestIsInProgress = false

    private var sendTariButtonIsVisible = true
    private var isServiceConnected = false

    private var walletService: TariWalletService? = null
    private var handler: Handler? = Handler(Looper.getMainLooper())

    // whether the user is currently dragging the list view.
    private var isDragging = false
    private var sendTariButtonClickAnimIsRunning = false
    private val onboardingInterstitialTimeMs = 4500L
    private val secondUTXOImportDelayTimeMs = 2500L
    private val secondUTXOStoreModalDelayTimeMs = 3000L

    // this flag is set if the testnet
    private var testnetTariRequestIsWaitingOnConnection = false

    /**
     * This listener is used only to animate the visibility of the scroll depth gradient view.
     */
    private var recyclerViewScrollListener = RecyclerViewScrollListener(this)

    private lateinit var updateProgressViewController: UpdateProgressViewController

    private var currentDialog: Dialog? = null
    private var shakeDetector = ShakeDetector(this)

    // region lifecycle functions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityHomeBinding.inflate(layoutInflater).apply { setContentView(root) }
        appComponent.inject(this)
        overridePendingTransition(0, 0)

        // the code below will send the user back to the very startup of the app
        // if the app is opened through a deep link, but the user has not
        // authenticated yet - this also includes the case of no wallet
        if (!sharedPrefsWrapper.isAuthenticated) {
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            // finish this activity
            finish()
            return
        }
        setupUi()
        subscribeToEventBus()
        tracker.screen("/home", "Home - Transaction List")
    }

    override fun onStart() {
        super.onStart()
        bindToWalletService()
        startShakeDetector()
    }

    override fun onStop() {
        handler?.removeCallbacksAndMessages(null)
        shakeDetector.stop()
        super.onStop()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        EventBus.unsubscribe(this)
        EventBus.unsubscribeFromNetworkConnectionState(this)
        handler = null
        ui.txRecyclerView.layoutManager = null
        ui.txRecyclerView.adapter = null
        unsetTouchListeners()
        if (walletService != null) {
            unbindService(this)
        }
        if (::updateProgressViewController.isInitialized) {
            updateProgressViewController.destroy()
        }
        super.onDestroy()
    }

    /**
     * Shaking the device should take the user to the debug screen.
     */
    private fun startShakeDetector() {
        (getSystemService(SENSOR_SERVICE) as? SensorManager)?.let { sensorManager ->
            shakeDetector.start(sensorManager)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i(
            "Debug",
            "HomeActivity onNewIntent: $intent\nData: ${intent?.data}${intent?.extras?.keySet()
                ?.joinToString { "\n$it = ${intent.extras!!.get(it)}" } ?: ""}"
        )
        intent?.let {
            handler?.postDelayed({ processIntentDeepLink(it) }, Constants.UI.mediumDurationMs)
        }
    }

    private fun processIntentDeepLink(intent: Intent) {
        // go to appropriate activity/fragment if the deep link in the intent
        // corresponds to a public key
        DeepLink.from(intent.data?.toString() ?: "")?.let { deepLink ->
            val pubkey = when (deepLink.type) {
                DeepLink.Type.EMOJI_ID -> walletService?.getPublicKeyFromEmojiId(deepLink.identifier)
                DeepLink.Type.PUBLIC_KEY_HEX -> walletService?.getPublicKeyFromHexString(deepLink.identifier)
            }
            pubkey?.let { publicKey -> sendTariToUser(publicKey, deepLink.parameters) }
        }
    }

    // endregion

    // region initial setup (UI and else)
    private fun setupUi() {
        /* commented out to fix the UI cutout issue
        makeStatusBarTransparent() --
        ViewCompat.setOnApplyWindowInsetsListener(ui.rootView) { _, insets ->
            insets.consumeSystemWindowInsets()
        }
         */

        // hide tx list header
        UiUtil.setTopMargin(ui.txListHeaderView, -dimenPx(R.dimen.common_header_height))

        updateProgressViewController = UpdateProgressViewController(
            ui.updateProgressContentView,
            this
        )
        ui.scrollView.bindUI()
        ui.scrollView.listenerWeakReference = WeakReference(this)
        ui.scrollView.updateProgressViewController = updateProgressViewController
        setupRecyclerView()
        setTouchListeners()

        ui.apply {

            headerElevationView.alpha = 0f
            sendTariButton.alpha = 0f
            sendTariButtonGradientView.alpha = 0f

            availableBalanceTextView.alpha = 0f
            storeImageView.alpha = 0f
            walletInfoImageView.alpha = 0f
            balanceGemImageView.alpha = 0f
            noTxsInfoTextView.gone()

            scrollContentView.alpha = 0f
            topContentContainerView.invisible()

            onboardingContentView.gone()

            scrollView.post { setupScrollView() }
            storeButton.setOnClickListener(this@HomeActivity::onStoreButtonClicked)
            walletInfoButton.setOnClickListener(this@HomeActivity::onWalletInfoButtonClicked)
            sendTariButton.setOnClickListener(this@HomeActivity::sendTariButtonClicked)
            closeTxListButton.setOnClickListener(this@HomeActivity::minimizeListButtonClicked)
            grabberContainerView.setOnClickListener(this@HomeActivity::grabberContainerViewClicked)
            grabberContainerView.setOnLongClickListener {
                grabberContainerViewLongClicked()
                true
            }
        }
    }

    private fun setupRecyclerView() {
        // initialize recycler view
        recyclerViewLayoutManager = LinearLayoutManager(this)
        ui.txRecyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter =
            TxListAdapter(cancelledTxs, completedTxs, pendingInboundTxs, pendingOutboundTxs) {
                Logger.i("Transaction with id ${it.id} selected.")
                startActivity(TxDetailActivity.createIntent(this, it))
            }
        ui.txRecyclerView.adapter = recyclerViewAdapter
        // hide vertical scrollbar initially
        ui.txRecyclerView.isVerticalScrollBarEnabled = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListeners() {
        ui.scrollBgEnablerView.setOnTouchListener(this)
        ui.scrollView.setOnTouchListener(this)
        ui.txRecyclerView.setOnTouchListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun unsetTouchListeners() {
        ui.scrollBgEnablerView.setOnTouchListener(null)
        ui.scrollView.setOnTouchListener(null)
        ui.txRecyclerView.setOnTouchListener(null)
    }

    private fun setupScrollView() {
        val recyclerViewHeight = ui.rootView.height - dimenPx(R.dimen.common_header_height)
        val contentHeight =
            dimenPx(R.dimen.home_tx_list_container_minimized_top_margin) +
                    dimenPx(R.dimen.home_grabber_container_height) + recyclerViewHeight
        UiUtil.setHeight(ui.recyclerViewContainerView, recyclerViewHeight)
        UiUtil.setHeight(ui.scrollContentView, contentHeight)
        ui.scrollView.recyclerViewContainerInitialHeight = recyclerViewHeight
        ui.scrollView.scrollToTop()

        ui.scrollView.setOnScrollChangeListener(this)
        ui.txRecyclerView.setOnScrollChangeListener(this)
        ui.txRecyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    private fun subscribeToEventBus() {
        // app events
        EventBus.subscribe<Event.App.AppForegrounded>(this) {
            if (walletService != null
                && updateProgressViewController.state == UpdateProgressViewController.State.IDLE
            ) {
                handler?.post {
                    updateProgressViewController.reset()
                    updateProgressViewController.start(walletService!!)
                    ui.scrollView.beginUpdate()
                }
            }
        }

        // wallet events
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
        EventBus.subscribe<Event.Wallet.TxMined>(this) {
            onTxMined(it.tx)
        }
        EventBus.subscribe<Event.Wallet.TxCancelled>(this) {
            if (updateProgressViewController.state != UpdateProgressViewController.State.RECEIVING) {
                onTxCancelled(it.tx)
            }
        }

        // Testnet Tari events
        EventBus.subscribe<Event.Testnet.TestnetTariRequestSuccessful>(this) {
            handler?.post { testnetTariRequestSuccessful() }
        }
        EventBus.subscribe<Event.Testnet.TestnetTariRequestError>(this) { event ->
            handler?.post { testnetTariRequestError(event.errorMessage) }
        }

        // tx-related app events
        EventBus.subscribe<Event.Tx.TxSendSuccessful>(this) {
            handler?.post { onTxSendSuccessful(it.txId) }
        }
        EventBus.subscribe<Event.Tx.TxSendFailed>(this) { event ->
            handler?.post { onTxSendFailed(event.failureReason) }
        }

        // network connection event
        EventBus.subscribeToNetworkConnectionState(this) { networkConnectionState ->
            if (testnetTariRequestIsWaitingOnConnection
                && networkConnectionState == NetworkConnectionState.CONNECTED
            ) {
                requestTestnetTari()
            }
        }

        // other app-specific events
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
        // update balance
        lifecycleScope.launch(Dispatchers.IO) {
            updateBalanceInfoData()
            withContext(Dispatchers.Main) {
                updateBalanceInfoUI(restart = false)
                showWalletBackupPromptIfNecessary()
            }
        }
        // update tx list UI
        handler?.post { updateTxListUI() }
    }

    private fun showWalletBackupPromptIfNecessary() {
        if (!sharedPrefsWrapper.backupIsEnabled || sharedPrefsWrapper.backupPassword == null) {
            val inboundNonFaucetTransactionsCount = countInboundNonFaucetTransaction()
            val tarisAmount =
                balanceInfo.availableBalance.tariValue + balanceInfo.pendingIncomingBalance.tariValue
            when {
                inboundNonFaucetTransactionsCount >= 5
                        && tarisAmount > BigDecimal("25000")
                        && sharedPrefsWrapper.backupIsEnabled
                        && sharedPrefsWrapper.backupPassword == null -> showSecureYourBackupsDialog()
                inboundNonFaucetTransactionsCount >= 4
                        && tarisAmount > BigDecimal("8000")
                        && !sharedPrefsWrapper.backupIsEnabled -> showRepeatedBackUpPrompt()
                inboundNonFaucetTransactionsCount >= 1
                        && !sharedPrefsWrapper.backupIsEnabled -> showInitialBackupPrompt()
            }
        }
    }

    private fun countInboundNonFaucetTransaction(): Int {
        val completedInboundNonFaucetTxs = completedTxs
            .filter { it.direction == Tx.Direction.INBOUND }
            .filterNot { it.status == TxStatus.IMPORTED }
            .count()
        return completedInboundNonFaucetTxs + pendingInboundTxs.size
    }

    private fun showInitialBackupPrompt() {
        BackupWalletPrompt(
            context = this,
            regularTitlePart = string(home_back_up_wallet_initial_title_regular_part),
            highlightedTitlePart = string(home_back_up_wallet_initial_title_highlighted_part),
            description = string(home_back_up_wallet_initial_description)
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun showRepeatedBackUpPrompt() {
        BackupWalletPrompt(
            context = this,
            regularTitlePart = string(home_back_up_wallet_repeated_title_regular_part),
            highlightedTitlePart = string(home_back_up_wallet_repeated_title_highlighted_part),
            description = string(home_back_up_wallet_repeated_description)
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun showSecureYourBackupsDialog() {
        BackupWalletPrompt(
            context = this,
            title = string(home_back_up_wallet_encrypt_title),
            description = string(home_back_up_wallet_encrypt_description),
            ctaText = string(home_back_up_wallet_encrypt_cta),
            dismissText = string(home_back_up_wallet_delay_encrypt_cta)
        ).asAndroidDialog()
            .apply(::replaceDialog)
    }

    private fun replaceDialog(dialog: Dialog) {
        currentDialog?.dismiss()
        currentDialog = dialog.also { it.show() }
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        // just update data - no UI change required
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        // just update data - no UI change required
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        // just update data - no UI change required
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        // just update data - no UI change required
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onTxMined(tx: CompletedTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        completedTxs.add(tx)
        // update tx list UI
        handler?.post { updateTxListUI() }
    }

    private fun onTxCancelled(tx: CancelledTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        cancelledTxs.add(tx)
        // update balance
        lifecycleScope.launch(Dispatchers.IO) {
            updateBalanceInfoData()
            handler?.post { updateBalanceInfoUI(restart = false) }
        }
        // update tx list UI
        handler?.post { updateTxListUI() }
    }

    private fun onContactAddedOrUpdated(publicKey: PublicKey, alias: String) {
        val contact = Contact(publicKey, alias)
        (cancelledTxs + pendingInboundTxs + pendingOutboundTxs + completedTxs).filter {
            it.user.publicKey == publicKey
        }.forEach {
            it.user = contact
        }
        handler?.post { updateTxListUI() }
    }

    private fun onContactRemoved(publicKey: PublicKey) {
        // non-contact user
        val user = User(publicKey)
        // update txs
        (cancelledTxs + pendingInboundTxs + pendingOutboundTxs + completedTxs).filter {
            it.user.publicKey == publicKey
        }.forEach {
            it.user = user
        }
        // update UI
        handler?.post { updateTxListUI() }
    }

    // region service connection

    private fun bindToWalletService() {
        // start service if not started yet
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service.")
        isServiceConnected = true
        walletService = TariWalletService.Stub.asInterface(service)

        lifecycleScope.launch(Dispatchers.IO) {
            // update data
            updateTxListData()
            // balance info
            updateBalanceInfoData()
            // init list
            handler?.post { initializeTxListUI() }
        }
        // check the start-up intent for a deep link
        handler?.postDelayed({ processIntentDeepLink(intent) }, Constants.UI.xLongDurationMs)
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        walletService = null
        isServiceConnected = false
    }

    // endregion

    private val txListIsEmpty: Boolean
        get() = cancelledTxs.isEmpty()
                && completedTxs.isEmpty()
                && pendingInboundTxs.isEmpty()
                && pendingOutboundTxs.isEmpty()

    /**
     * Fetches transactions from the service & updates the lists.
     */
    private fun updateTxListData() {
        val error = WalletError()
        val service = walletService!!
        val walletCanceledTxs = service.getCancelledTxs(error)
        val walletCompletedTxs = service.getCompletedTxs(error)
        val walletPendingInboundTxs = service.getPendingInboundTxs(error)
        val walletPendingOutboundTxs = service.getPendingOutboundTxs(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        cancelledTxs.repopulate(walletCanceledTxs)
        completedTxs.repopulate(walletCompletedTxs)
        pendingInboundTxs.repopulate(walletPendingInboundTxs)
        pendingOutboundTxs.repopulate(walletPendingOutboundTxs)
    }

    private fun initializeTxListUI() {
        if (txListIsEmpty && !sharedPrefsWrapper.onboardingDisplayedAtHome) {
            isOnboarding = true
            playOnboardingAnim()
            sharedPrefsWrapper.onboardingDisplayedAtHome = true
        } else { // past onboarding
            // display txs
            recyclerViewAdapter.notifyDataChanged()
            playNonOnboardingStartupAnim()
            if (txListIsEmpty) {
                showNoTxsTextView()
            }
            if (!sharedPrefsWrapper.faucetTestnetTariRequestCompleted) {
                requestTestnetTari()
            }
            updateProgressViewController.reset()
            ui.scrollView.beginUpdate()
            updateProgressViewController.start(walletService!!)
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

    private fun updateBalanceInfoUI(restart: Boolean) {
        if (restart) {
            ui.balanceDigitContainerView.removeAllViews()
            ui.balanceDecimalsDigitContainerView.removeAllViews()
            balanceViewController =
                BalanceViewController(
                    this,
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

        // initialize the balance view controller
        updateBalanceInfoUI(true)

        // show button and list
        val sendTariButtonMarginDelta =
            dimenPx(R.dimen.home_send_tari_button_visible_bottom_margin) - dimenPx(R.dimen.home_send_tari_button_initial_bottom_margin)
        val anim = ValueAnimator.ofFloat(
            0.0f,
            1.0f
        )

        ui.scrollView.scrollToTop()
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            // value will run from 0.0 to 1.0
            val value = valueAnimator.animatedValue as Float

            // animate the list (will move upwards)
            ui.scrollContentView.y =
                dimenPx(R.dimen.home_scroll_view_startup_anim_height) * (1 - value)
            ui.scrollContentView.alpha = value
            ui.sendTariButton.alpha = value

            // animate the send tari button (will move upwards)
            UiUtil.setBottomMargin(
                ui.sendTariButton,
                (dimenPx(R.dimen.home_send_tari_button_initial_bottom_margin) + value * sendTariButtonMarginDelta).toInt()
            )
            ui.sendTariButtonGradientView.alpha = value
            // reveal balance title, QR code button and balance gem image
            ui.availableBalanceTextView.alpha = value
            ui.storeImageView.alpha = value
            ui.walletInfoImageView.alpha = value
            ui.balanceGemImageView.alpha = value
        }
        sendTariButtonIsVisible = true
        anim.duration = Constants.UI.Home.startupAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        ui.blockerView.postDelayed({
            ui.blockerView.gone()
        }, Constants.UI.Home.startupAnimDurationMs)
        anim.start()
    }

    /**
     * Play onboarding animation.
     */
    private fun playOnboardingAnim() {
        ui.onboardingContentView.visible()
        hideSendTariButtonAnimated()

        updateBalanceInfoData()
        // initialize the balance view controller
        balanceViewController =
            BalanceViewController(
                this,
                ui.balanceDigitContainerView,
                ui.balanceDecimalsDigitContainerView,
                balanceInfo // initial value
            )

        ui.scrollView.scrollTo(0, ui.scrollView.height)
        ui.scrollContentView.alpha = 1f
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
        val blackBgViewFadeAnim = ValueAnimator.ofFloat(0f, 1f)
        blackBgViewFadeAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.txListOverlayView.alpha = value
        }
        // the animation set
        val animSet = AnimatorSet()
        animSet.playTogether(
            scrollViewTransAnim,
            blackBgViewFadeAnim
        )
        animSet.duration = Constants.UI.Home.welcomeAnimationDurationMs
        animSet.addListener(
            onEnd = {
                ui.topContentContainerView.visible()
                ui.availableBalanceTextView.alpha = 1f
                ui.storeImageView.alpha = 1f
                ui.walletInfoImageView.alpha = 1f
                ui.balanceGemImageView.alpha = 1f
            }
        )
        animSet.start()

        // show interstitial & finish after delay
        ui.scrollView.isScrollable = false
        ui.scrollView.postDelayed({
            ui.scrollView.smoothScrollTo(0, 0)
            ui.blockerView.gone()
        }, onboardingInterstitialTimeMs)

    }

    private fun showNoTxsTextView() {
        ui.noTxsInfoTextView.alpha = 0f
        ui.noTxsInfoTextView.visible()
        val anim = ObjectAnimator.ofFloat(
            ui.noTxsInfoTextView,
            "alpha",
            0f, 1f
        )
        anim.duration = Constants.UI.mediumDurationMs
        anim.start()
    }

    private fun requestTestnetTari() {
        if (testnetTariRequestIsInProgress) {
            return
        }
        if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
            testnetTariRequestIsWaitingOnConnection = true
            return
        }
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

    private fun testnetTariRequestSuccessful() {
        val error = WalletError()
        val importedTx =
            walletService!!.importTestnetUTXO(string(first_testnet_utxo_tx_message), error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        sharedPrefsWrapper.faucetTestnetTariRequestCompleted = true
        sharedPrefsWrapper.firstTestnetUTXOTxId = importedTx.id
        completedTxs.add(importedTx)
        lifecycleScope.launch(Dispatchers.IO) {
            updateBalanceInfoData()
            // update UI
            handler?.post {
                updateTxListUI()
                updateBalanceInfoUI(restart = false)
            }
            // display dialog
            handler?.postDelayed({
                showTestnetTariReceivedDialog(importedTx.user.publicKey)
            }, Constants.UI.Home.showTariBotDialogDelayMs)
            testnetTariRequestIsInProgress = false
        }
    }

    private fun testnetTariRequestError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        testnetTariRequestIsInProgress = false
        showSendTariButtonAnimated()
    }

    private fun showTestnetTariReceivedDialog(testnetSenderPublicKey: PublicKey) {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = this,
            layoutId = R.layout.home_dialog_testnet_tari_received,
            canceledOnTouchOutside = false
        ).apply {
            findViewById<TextView>(R.id.home_testnet_tari_received_dlg_txt_title).text =
                string(home_tari_bot_you_got_tari_dlg_title).applyFontStyle(
                    this@HomeActivity,
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    string(home_tari_bot_you_got_tari_dlg_title_bold_part),
                    CustomFont.AVENIR_LT_STD_BLACK
                )
            findViewById<TextView>(R.id.home_tari_bot_dialog_txt_try_later)
                .setOnClickListener {
                    currentDialog = null
                    dismiss()
                    ui.rootView.postDelayed({
                        showSendTariButtonAnimated()
                    }, Constants.UI.shortDurationMs)
                }
            findViewById<TextView>(R.id.home_tari_bot_dialog_btn_send_tari)
                .setOnClickListener {
                    currentDialog = null
                    dismiss()
                    sendTariToUser(testnetSenderPublicKey)
                    ui.rootView.postDelayed({
                        showSendTariButtonAnimated()
                    }, Constants.UI.longDurationMs)

                }
        }.apply { show() }
            .asAndroidDialog()
    }

    private fun showTTLStoreDialog() {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = this,
            layoutId = R.layout.home_dialog_ttl_store
        ).apply {
            findViewById<TextView>(R.id.home_ttl_store_dialog_txt_title).text =
                string(home_ttl_store_dlg_title).applyFontStyle(
                    this@HomeActivity,
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    string(home_ttl_store_dlg_title_bold_part),
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
                    handler?.postDelayed({
                        currentDialog = null
                        dismiss()
                    }, Constants.UI.mediumDurationMs)
                }
            findViewById<View>(R.id.home_ttl_store_dialog_vw_top_spacer)
                .setOnClickListener { dismiss() }
        }.apply { show() }
            .asAndroidDialog()
    }

    private fun visitTTLStore() {
        StoreDialogFragment.newInstance().show(supportFragmentManager, null)
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
        val intent = Intent(this, SendTariActivity::class.java)
        intent.putExtra("recipientUser", recipientUser)
        parameters[DeepLink.PARAMETER_NOTE]?.let { intent.putExtra(DeepLink.PARAMETER_NOTE, it) }
        parameters[DeepLink.PARAMETER_AMOUNT]?.toDoubleOrNull()
            ?.let { intent.putExtra(DeepLink.PARAMETER_AMOUNT, it) }
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    /**
     * Displays the store modal on store button click.
     */
    private fun onStoreButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showTTLStoreDialog()
    }

    /**
     * Opens user wallet info on button click.
     */
    private fun onWalletInfoButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val intent = Intent(this@HomeActivity, WalletInfoActivity::class.java)
        startActivity(intent)
    }

    private fun sendTariButtonClicked(view: View) {
        if (EventBus.networkConnectionStateSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this)
            return
        }
        UiUtil.temporarilyDisableClick(view)
        animateSendTariButtonOnClick(
            Constants.UI.Button.clickScaleAnimFullScale,
            Constants.UI.Button.clickScaleAnimSmallScale
        )
    }

    private fun endOnboarding() {
        isOnboarding = false
        ui.onboardingContentView.gone()
        ui.txListHeaderView.visible()
        lifecycleScope.launch(Dispatchers.IO) {
            updateTxListData()
            if (isActive) {
                updateBalanceInfoData()
                handler?.post {
                    updateBalanceInfoUI(restart = false)
                    updateTxListUI()
                }
            }
        }
        // request Testnet Tari if no txs
        if (txListIsEmpty) {
            handler?.postDelayed({
                requestTestnetTari()
            }, Constants.UI.xxLongDurationMs)
        } else {
            showSendTariButtonAnimated()
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
            handler?.post {
                updateBalanceInfoUI(restart = true)
                updateTxListUI()
            }
        }

        // import second testnet UTXO if it hasn't been imported yet
        if (sharedPrefsWrapper.testnetTariUTXOKeyList.isNotEmpty()) {
            handler?.postDelayed({
                importSecondUTXO()
            }, secondUTXOImportDelayTimeMs)
        }
    }

    /**
     * Called when an outgoing transaction has failed.
     */
    private fun onTxSendFailed(failureReason: FinalizeSendTxFragment.FailureReason) {
        when (failureReason) {
            NETWORK_CONNECTION_ERROR -> {
                displayNetworkConnectionErrorDialog()
            }
            BASE_NODE_CONNECTION_ERROR, SEND_ERROR -> {
                displayBaseNodeConnectionErrorDialog()
            }
        }
    }

    private fun importSecondUTXO() {
        val error = WalletError()
        val importedTx = walletService!!.importTestnetUTXO(
            string(second_testnet_utxo_tx_message),
            error
        )
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        sharedPrefsWrapper.secondTestnetUTXOTxId = importedTx.id
        completedTxs.add(importedTx)
        lifecycleScope.launch(Dispatchers.IO) {
            updateBalanceInfoData()
            // update UI
            handler?.post {
                updateTxListUI()
                updateBalanceInfoUI(restart = false)
            }
            // display store dialog
            handler?.postDelayed({
                showTTLStoreDialog()
            }, secondUTXOStoreModalDelayTimeMs)
        }
    }

    private fun minimizeListButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        ui.scrollView.smoothScrollTo(0, 0)
        ui.txRecyclerView.smoothScrollToPosition(0)
    }

    private fun grabberContainerViewClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        ui.scrollView.smoothScrollTo(
            0,
            ui.scrollContentView.height - ui.scrollView.height
        )
    }

    /**
     * A shake will take the user to the debug screen.
     */
    override fun hearShake() {
        val intent = Intent(this, DebugActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun grabberContainerViewLongClicked() {
        val intent = Intent(this, DebugActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    /**
     * Reveals the send tari button with animation after a specified delay time in ms.
     */
    private fun showSendTariButtonAnimated() {
        if (sendTariButtonIsVisible) {
            return
        }
        ui.sendTariButton.alpha = 1f
        val initialMargin = UiUtil.getBottomMargin(ui.sendTariButton)
        val marginDelta =
            dimenPx(R.dimen.home_send_tari_button_visible_bottom_margin) - UiUtil.getBottomMargin(ui.sendTariButton)
        val anim = ValueAnimator.ofFloat(
            0f,
            1f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                ui.sendTariButton,
                (initialMargin + marginDelta * value).toInt()
            )
            ui.sendTariButtonGradientView.alpha = value
        }
        anim.duration = Constants.UI.mediumDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()
        sendTariButtonIsVisible = true
    }

    /**
     * Hides the send tari button with animation after a specified delay time in ms.
     */
    private fun hideSendTariButtonAnimated() {
        if (!sendTariButtonIsVisible) {
            return
        }
        val initialMargin = UiUtil.getBottomMargin(ui.sendTariButton)
        val marginDelta =
            dimenPx(R.dimen.home_send_tari_button_hidden_bottom_margin) - UiUtil.getBottomMargin(ui.sendTariButton)
        val anim = ValueAnimator.ofFloat(
            0f,
            1f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                ui.sendTariButton,
                (initialMargin + marginDelta * value).toInt()
            )
            ui.sendTariButtonGradientView.alpha = 1f - value
        }
        anim.duration = Constants.UI.mediumDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()
        sendTariButtonIsVisible = false
    }

    private fun animateSendTariButtonOnClick(startScale: Float, endScale: Float) {
        val anim: Animation = ScaleAnimation(
            startScale, endScale,  // start and end values for the X axis scaling
            startScale, endScale,  // start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0.5f,  // pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 0.5f // pivot point of Y scaling
        )
        anim.fillAfter = true // keeps the result of the animation
        if (!sendTariButtonClickAnimIsRunning) {
            anim.interpolator = DecelerateInterpolator()
            sendTariButtonClickAnimIsRunning = true
            anim.duration = Constants.UI.Button.clickScaleAnimDurationMs
            anim.startOffset = Constants.UI.Button.clickScaleAnimStartOffset
        } else {
            anim.interpolator = AccelerateInterpolator()
            anim.duration = Constants.UI.Button.clickScaleAnimReturnDurationMs
            anim.startOffset = Constants.UI.Button.clickScaleAnimReturnStartOffset
        }
        anim.setAnimationListener(this)
        ui.sendTariButton.startAnimation(anim)
    }

    override fun onSwipeRefresh(source: CustomScrollView) {
        ui.rootView.post {
            updateProgressViewController.start(walletService!!)
        }
    }

    override fun updateHasFailed(
        source: UpdateProgressViewController,
        failureReason: UpdateProgressViewController.FailureReason
    ) {
        handler?.post {
            ui.scrollView.finishUpdate()
            when (failureReason) {
                UpdateProgressViewController.FailureReason.NETWORK_CONNECTION_ERROR -> {
                    displayNetworkConnectionErrorDialog()
                }
                UpdateProgressViewController.FailureReason.BASE_NODE_CONNECTION_ERROR -> {
                    displayBaseNodeConnectionErrorDialog()
                }
            }
        }
    }

    override fun updateHasCompleted(
        source: UpdateProgressViewController,
        receivedTxCount: Int,
        cancelledTxCount: Int
    ) {
        handler?.post { ui.scrollView.finishUpdate() }
        if (receivedTxCount > 0 || cancelledTxCount > 0) {
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
    }

    private fun displayNetworkConnectionErrorDialog() {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = this,
            layoutId = R.layout.tx_failed_dialog,
            dismissViewId = R.id.tx_failed_dialog_txt_close
        ).apply {
            val titleTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_title)
            titleTextView.text = string(error_no_connection_title)
            val descriptionTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_description)
            descriptionTextView.text = string(error_no_connection_description)
            // set text
        }.apply { show() }
            .asAndroidDialog()
            .apply { setOnDismissListener { currentDialog = null } }
    }

    private fun displayBaseNodeConnectionErrorDialog() {
        currentDialog?.dismiss()
        currentDialog = BottomSlideDialog(
            context = this,
            layoutId = R.layout.tx_failed_dialog,
            dismissViewId = R.id.tx_failed_dialog_txt_close
        ).apply {
            val titleTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_title)
            titleTextView.text = string(error_node_unreachable_title)
            val descriptionTextView = findViewById<TextView>(R.id.tx_failed_dialog_txt_description)
            descriptionTextView.text = string(error_node_unreachable_description)
            // set text
        }.apply { show() }
            .asAndroidDialog()
        currentDialog?.setOnDismissListener {
            currentDialog = null
        }
    }

    // region send tari button animation listener

    override fun onAnimationStart(animation: Animation?) {
        // no-op
    }

    override fun onAnimationRepeat(animation: Animation?) {
        // no-op
    }

    /**
     * Send Tari button click animation has completed half or full cycle.
     */
    override fun onAnimationEnd(animation: Animation?) {
        if (sendTariButtonClickAnimIsRunning) { // bounce back the button
            animateSendTariButtonOnClick(
                Constants.UI.Button.clickScaleAnimSmallScale,
                Constants.UI.Button.clickScaleAnimFullScale
            )
            sendTariButtonClickAnimIsRunning = false
        } else { // animation is over, go to send activity
            // go to fragment
            val intent = Intent(this@HomeActivity, SendTariActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
    }

    // endregion

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {

        if (view == null || event == null) {
            return false
        }
        if (view === ui.scrollView || view === ui.txRecyclerView) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                }
                MotionEvent.ACTION_UP -> {
                    if (ui.scrollView.scrollY == 0 && !ui.txRecyclerView.isScrolledToTop()) {
                        ui.txRecyclerView.smoothScrollToPosition(0)
                        handler?.postDelayed({
                            recyclerViewScrollListener.reset()
                            ui.headerElevationView.alpha = 0f
                        }, Constants.UI.mediumDurationMs)
                    }
                    ui.scrollView.flingIsRunning = false
                    ui.scrollView.postDelayed({
                        ui.scrollView.completeScroll()
                    }, 50L)
                    isDragging = false
                }
            }
            return false
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
            UiUtil.setTopMargin(
                ui.topContentContainerView,
                dimenPx(R.dimen.home_top_content_container_view_top_margin)
                        + topContentMarginTopExtra
            )
            ui.txListTitleTextView.alpha = ratio
            ui.closeTxListButton.alpha = ratio

            if (!isOnboarding) {
                UiUtil.setTopMargin(
                    ui.txListHeaderView,
                    ((ratio - 1) * dimenPx(R.dimen.common_header_height)).toInt()
                )
                ui.grabberView.alpha = max(
                    0f,
                    1f - ratio * grabberViewAlphaScrollAnimCoefficient
                )

                UiUtil.setWidth(
                    ui.grabberView,
                    (max(
                        0f,
                        1f - ratio * grabberViewWidthScrollAnimCoefficient
                    ) * dimenPx(R.dimen.home_grabber_width)).toInt()
                )
                val grabberBgDrawable = ui.grabberContainerView.background as GradientDrawable
                grabberBgDrawable.cornerRadius = max(
                    0f,
                    1f - ratio * grabberViewCornerRadiusScrollAnimCoefficient
                ) * dimenPx(R.dimen.home_grabber_corner_radius)

                if (ratio == 0f && !isDragging && !ui.txRecyclerView.isScrolledToTop()) {
                    ui.txRecyclerView.smoothScrollToPosition(0)
                    handler?.postDelayed({
                        recyclerViewScrollListener.reset()
                        ui.headerElevationView.alpha = 0f
                    }, Constants.UI.mediumDurationMs)
                }
            } else if (ratio == 0f && !isDragging) { // is onboarding
                ui.scrollView.isScrollable = true
                endOnboarding()
            }

            UiUtil.setTopMargin(
                ui.storeButton,
                dimenPx(R.dimen.home_wallet_info_button_initial_top_margin) + ui.scrollView.scrollY + topContentMarginTopExtra
            )
            UiUtil.setTopMargin(
                ui.walletInfoButton,
                dimenPx(R.dimen.home_wallet_info_button_initial_top_margin) + ui.scrollView.scrollY + topContentMarginTopExtra
            )
        }
        if (scrollY > oldScrollY
            && !isOnboarding
            && !testnetTariRequestIsInProgress
        ) {
            hideSendTariButtonAnimated()
        } else if (scrollY < oldScrollY
            && !isOnboarding
            && !testnetTariRequestIsInProgress
        ) {
            showSendTariButtonAnimated()
        }
    }

    // region scroll depth gradient view controls

    fun onRecyclerViewScrolled(totalDeltaY: Int) {
        ui.headerElevationView.alpha = min(
            Constants.UI.scrollDepthShadowViewMaxOpacity,
            totalDeltaY / dimenPx(R.dimen.home_tx_list_item_height).toFloat()
        )
    }

    class RecyclerViewScrollListener(activity: HomeActivity) : RecyclerView.OnScrollListener() {

        private val activityWR = WeakReference(activity)
        private var totalDeltaY = 0

        fun reset() {
            totalDeltaY = 0
        }

        override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
            super.onScrolled(recyclerView, dX, dY)
            totalDeltaY += dY
            activityWR.get()?.onRecyclerViewScrolled(totalDeltaY)
        }

    }

    // endregion

    class BackupWalletPrompt private constructor(private val dialog: Dialog) {

        constructor(
            context: Context,
            title: CharSequence,
            description: CharSequence,
            ctaText: CharSequence = context.string(home_back_up_wallet_back_up_cta),
            dismissText: CharSequence = context.string(home_back_up_wallet_delay_back_up_cta)
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
                    context.startActivities(
                        arrayOf(
                            Intent(context, WalletInfoActivity::class.java),
                            Intent(context, SettingsActivity::class.java)
                        )
                    )
                    dismiss()
                }
            }.asAndroidDialog()
        )

        constructor(
            context: Context,
            regularTitlePart: CharSequence,
            highlightedTitlePart: CharSequence,
            description: CharSequence,
            ctaText: CharSequence = context.string(home_back_up_wallet_back_up_cta),
            dismissText: CharSequence = context.string(home_back_up_wallet_delay_back_up_cta)
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
            dismissText
        )

        fun asAndroidDialog() = dialog

    }

}
