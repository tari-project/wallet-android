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
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.activity.debug.DebugActivity
import com.tari.android.wallet.ui.activity.home.adapter.TxListAdapter
import com.tari.android.wallet.ui.activity.profile.WalletInfoActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.activity.tx.TxDetailActivity
import com.tari.android.wallet.ui.extension.scrollToTop
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Home activity - transaction list.
 *
 * @author The Tari Development Team
 */

internal class HomeActivity : BaseActivity(),
    ServiceConnection,
    SwipeRefreshLayout.OnRefreshListener,
    View.OnScrollChangeListener,
    View.OnTouchListener,
    Animation.AnimationListener,
    TxListAdapter.Listener {

    @BindView(R.id.home_vw_root)
    lateinit var rootView: View
    @BindView(R.id.home_vw_top_content_container)
    lateinit var topContentContainerView: View
    @BindView(R.id.home_vw_gradient_bg)
    lateinit var gradientBgView: View

    @BindView(R.id.home_swipe_container)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    @BindView(R.id.home_rv_tx_list)
    lateinit var recyclerView: RecyclerView

    // transaction list
    @BindView(R.id.home_vw_tx_list_header)
    lateinit var txListHeaderView: View
    @BindView(R.id.home_btn_close_tx_list)
    lateinit var minimizeTxListButton: ImageButton
    @BindView(R.id.home_txt_tx_list_title)
    lateinit var txListTitleTextView: TextView
    @BindView(R.id.home_vw_grabber_container)
    lateinit var grabberContainerView: View
    @BindView(R.id.home_vw_grabber)
    lateinit var grabberView: View
    @BindView(R.id.home_vw_tx_list_bg_overlay)
    lateinit var txListBgOverlayView: View
    @BindView(R.id.home_vw_header_elevation)
    lateinit var headerElevationView: View

    @BindView(R.id.home_btn_send_tari)
    lateinit var sendTariButton: Button
    @BindView(R.id.home_vw_send_tari_btn_bg_gradient)
    lateinit var sendTariButtonBgGradientView: View

    // Balance views.
    @BindView(R.id.home_txt_available_balance)
    lateinit var balanceTitleTextView: TextView
    @BindView(R.id.home_img_balance_gem)
    lateinit var balanceGemImageView: ImageView
    @BindView(R.id.home_img_wallet_info)
    lateinit var userWalletInfoButton: ImageView

    // Balance digit containers.
    @BindView(R.id.home_vw_balance_digit_container)
    lateinit var balanceDigitContainerView: ViewGroup
    @BindView(R.id.home_balance_vw_decimals_digit_container)
    lateinit var balanceDecimalDigitContainerView: ViewGroup

    @BindView(R.id.home_scroll_view)
    lateinit var scrollView: CustomScrollView
    @BindView(R.id.home_vw_scroll_bg_enabler)
    lateinit var scrollBgEnabler: View
    @BindView(R.id.home_vw_scroll_content)
    lateinit var scrollContentView: View

    // onboarding content
    @BindView(R.id.home_vw_onboarding_content)
    lateinit var onboardingContentView: ViewGroup

    @BindView(R.id.home_txt_no_txs_info)
    lateinit var noTxsInfoTextView: TextView

    @BindDimen(R.dimen.home_top_content_container_view_top_margin)
    @JvmField
    var topContentContainerViewTopMargin = 0
    @BindDimen(R.dimen.home_top_content_container_scroll_vertical_shift)
    @JvmField
    var topContentContainerViewScrollVerticalShift = 0

    @BindDimen(R.dimen.common_header_height)
    @JvmField
    var txListHeaderHeight = 0
    @BindDimen(R.dimen.home_send_tari_button_initial_bottom_margin)
    @JvmField
    var sendTariButtonInitialBottomMargin = 0
    @BindDimen(R.dimen.home_send_tari_button_visible_bottom_margin)
    @JvmField
    var sendTariButtonVisibleBottomMargin = 0
    @BindDimen(R.dimen.home_tx_list_container_minimized_top_margin)
    @JvmField
    var txListContainerMinimizedTopMargin = 0
    @BindDimen(R.dimen.home_scroll_view_startup_anim_height)
    @JvmField
    var scrollViewStartupAnimHeight = 0

    @BindDimen(R.dimen.home_grabber_container_height)
    @JvmField
    var grabberContainerHeight = 0
    @BindDimen(R.dimen.home_grabber_corner_radius)
    @JvmField
    var grabberCornerRadius = 0
    @BindDimen(R.dimen.home_send_tari_button_hidden_bottom_margin)
    @JvmField
    var sendTariButtonHiddenBottomMargin = 0
    @BindDimen(R.dimen.home_grabber_width)
    @JvmField
    var grabberViewWidth = 0
    @BindDimen(R.dimen.home_tx_list_item_height)
    @JvmField
    var listItemHeight = 0
    @BindDimen(R.dimen.home_main_content_top_margin)
    @JvmField
    var homeMainContentTopMargin = 0

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    @BindString(R.string.wallet_service_error_testnet_tari_request)
    @JvmField
    var homeFailToLoadTransaction = ""

    @BindString(R.string.wallet_service_error_no_internet_connection)
    @JvmField
    var homeNoInternetConnection = ""

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    // tx list
    private lateinit var recyclerViewAdapter: TxListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private val completedTxs = mutableListOf<CompletedTx>()
    private val pendingInboundTxs = mutableListOf<PendingInboundTx>()
    private val pendingOutboundTxs = mutableListOf<PendingOutboundTx>()
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
    private val wr = WeakReference(this)
    private val uiHandler = Handler(Looper.getMainLooper())
    private val handler = Handler()

    /**
     * Whether the user is currently dragging the list view.
     */
    private var isDragging = false

    private var sendTariButtonClickAnimIsRunning = false

    /**
     * This listener is used only to animate the visibility of the scroll depth gradient view.
     */
    private var scrollListener = ScrollListener(this)

    override val contentViewId = R.layout.activity_home

    // region lifecycle functions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        /* commented out to fix the UI cutout issue
        makeStatusBarTransparent() --
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            insets.consumeSystemWindowInsets()
        }
         */

        // hide tx list header
        UiUtil.setTopMargin(txListHeaderView, -txListHeaderHeight)

        setupSwipeRefreshLayout()
        setupRecyclerView()
        setTouchListeners()

        headerElevationView.alpha = 0f
        sendTariButton.alpha = 0f
        sendTariButtonBgGradientView.alpha = 0f

        balanceTitleTextView.alpha = 0f
        userWalletInfoButton.alpha = 0f
        balanceGemImageView.alpha = 0f
        noTxsInfoTextView.visibility = View.GONE

        scrollContentView.alpha = 0f
        topContentContainerView.visibility = View.INVISIBLE

        onboardingContentView.visibility = View.GONE

        scrollView.post {
            wr.get()?.setupScrollView()
        }

        subscribeToEventBus()
        TrackHelper.track()
            .screen("/home")
            .title("Home - Transaction List")
            .with(tracker)
    }

    override fun onStart() {
        super.onStart()
        bindToWalletService()
    }

    override fun onStop() {
        uiHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null
        unsetTouchListeners()
        unbindService(this)
        EventBus.unsubscribe(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        // we're at the root activity,
        // do nothing when back is pressed
    }

    // endregion

    // region initial setup (UI and else)

    private fun setupSwipeRefreshLayout() {
        // initialize pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this)
        // configure the refreshing colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.home_bg_gradient_start,
            R.color.home_bg_gradient_center,
            R.color.home_bg_gradient_end
        )
    }

    private fun setupRecyclerView() {
        // initialize recycler view
        recyclerViewLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = recyclerViewLayoutManager
        recyclerViewAdapter =
            TxListAdapter(
                completedTxs,
                pendingInboundTxs,
                pendingOutboundTxs,
                this
            )
        recyclerView.adapter = recyclerViewAdapter
        // recycler view is initially disabled
        recyclerView.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListeners() {
        scrollBgEnabler.setOnTouchListener(this)
        scrollView.setOnTouchListener(this)
        recyclerView.setOnTouchListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun unsetTouchListeners() {
        scrollBgEnabler.setOnTouchListener(null)
        scrollView.setOnTouchListener(null)
        recyclerView.setOnTouchListener(null)
    }

    private fun setupScrollView() {
        val recyclerViewHeight = rootView.height - txListHeaderHeight
        val contentHeight =
            txListContainerMinimizedTopMargin + grabberContainerHeight + recyclerViewHeight
        UiUtil.setHeight(swipeRefreshLayout, recyclerViewHeight)
        UiUtil.setHeight(onboardingContentView, recyclerViewHeight)
        UiUtil.setHeight(scrollContentView, contentHeight)
        scrollView.scrollToTop()

        scrollView.setOnScrollChangeListener(this)
        recyclerView.setOnScrollChangeListener(this)
        recyclerView.addOnScrollListener(scrollListener)
    }

    private fun subscribeToEventBus() {
        // event bus subscriptions
        EventBus.subscribe<Event.Tx.TxSendSuccessful>(this) {
            wr.get()?.rootView?.post {
                wr.get()?.onSendTxSuccessful()
            }
        }
        EventBus.subscribe<Event.Testnet.TestnetTariRequestSuccessful>(this) { event ->
            wr.get()?.rootView?.post {
                wr.get()?.testnetTariRequestSuccessful(event.senderPublicKey)
            }
        }
        EventBus.subscribe<Event.Testnet.TestnetTariRequestError>(this) { event ->
            wr.get()?.rootView?.post {
                wr.get()?.testnetTariRequestError(event.errorMessage)
            }
        }
        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) {
            wr.get()?.rootView?.post {
                updateAllDataAndUI(restartBalanceUI = false)
            }
        }
        EventBus.subscribe<Event.Wallet.TxReceived>(this) {
            wr.get()?.rootView?.post {
                updateAllDataAndUI(restartBalanceUI = false)
            }
        }
    }

    // endregion

    // region service connection

    private fun bindToWalletService() {
        // start service if not started yet
        if (walletService == null) {
            // start the wallet service
            ContextCompat.startForegroundService(
                this,
                Intent(this, WalletService::class.java)
            )
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            //intent.action = TariWalletService::class.java.name
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

        AsyncTask.execute {
            // update data
            updateTxListData()
            // balance info
            updateBalanceInfoData()
            // init list
            wr.get()?.rootView?.post {
                wr.get()?.initializeTxListUI()
            }
        }
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

    /**
     * Called on swipe refresh.
     */
    override fun onRefresh() {
        recyclerView.isNestedScrollingEnabled = false
        AsyncTask.execute {
            wr.get()?.updateAllDataAndUI(restartBalanceUI = false)
        }
    }

    private val txListIsEmpty: Boolean
        get() {
            return completedTxs.isEmpty()
                    && pendingInboundTxs.isEmpty()
                    && pendingOutboundTxs.isEmpty()
        }

    /**
     * Fetches transactions from the service & updates the lists.
     */
    private fun updateTxListData(): Boolean {
        val error = WalletError()
        val walletCompletedTxs = walletService!!.getCompletedTxs(error)
        val walletPendingInboundTxs = walletService!!.getPendingInboundTxs(error)
        val walletPendingOutboundTxs = walletService!!.getPendingOutboundTxs(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        completedTxs.clear()
        completedTxs.addAll(walletCompletedTxs)
        pendingInboundTxs.clear()
        pendingInboundTxs.addAll(walletPendingInboundTxs)
        pendingOutboundTxs.clear()
        pendingOutboundTxs.addAll(walletPendingOutboundTxs)
        return true
    }

    /**
     * TODO
     * Prepares the
     */
    private fun initializeTxListUI() {
        if (txListIsEmpty && !sharedPrefsWrapper.onboardingDisplayedAtHome) {
            isOnboarding = true
            playOnboardingAnim()
            sharedPrefsWrapper.onboardingDisplayedAtHome = true
        } else {
            // display txs
            recyclerViewAdapter.notifyDataChanged()
            playNonOnboardingStartupAnim()
            if (txListIsEmpty) {
                showNoTxsTextView()
                AsyncTask.execute {
                    wr.get()?.requestTestnetTari()
                }

            }
        }
    }

    private fun updateTxListUI() {
        recyclerViewAdapter.notifyDataChanged()
        swipeRefreshLayout.isRefreshing = false
        recyclerView.isNestedScrollingEnabled = true
        scrollListener.reset()
        if (txListIsEmpty) {
            showNoTxsTextView()
        } else {
            noTxsInfoTextView.visibility = View.GONE
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
            balanceDigitContainerView.removeAllViews()
            balanceDecimalDigitContainerView.removeAllViews()
            balanceViewController =
                BalanceViewController(
                    this,
                    balanceDigitContainerView,
                    balanceDecimalDigitContainerView,
                    balanceInfo
                )
            // show digits
            balanceViewController.runStartupAnimation()
        } else {
            balanceViewController.balanceInfo = balanceInfo
        }
    }

    /**
     * Updates all displayed data & UI.
     *
     * @param restartBalanceUI plays the balance animation anew
     */
    private fun updateAllDataAndUI(restartBalanceUI: Boolean) {
        AsyncTask.execute {
            updateTxListData()
            updateBalanceInfoData()
            wr.get()?.rootView?.post {
                wr.get()?.updateBalanceInfoUI(restartBalanceUI)
                wr.get()?.updateTxListUI()
            }
        }
    }

    /**
     * The startup animation - reveals the list, balance and other views.
     */
    private fun playNonOnboardingStartupAnim() {
        topContentContainerView.visibility = View.VISIBLE

        // initialize the balance view controller
        updateBalanceInfoUI(true)

        // show button and list
        val sendTariButtonMarginDelta =
            sendTariButtonVisibleBottomMargin - sendTariButtonInitialBottomMargin
        val anim = ValueAnimator.ofFloat(
            0.0f,
            1.0f
        )

        scrollView.scrollToTop()
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            // value will run from 0.0 to 1.0
            val value = valueAnimator.animatedValue as Float

            // animate the list (will move upwards)
            scrollContentView.y = scrollViewStartupAnimHeight * (1 - value)
            scrollContentView.alpha = value
            sendTariButton.alpha = value

            // animate the send tari button (will move upwards)
            UiUtil.setBottomMargin(
                sendTariButton,
                (sendTariButtonInitialBottomMargin + value * sendTariButtonMarginDelta).toInt()
            )
            sendTariButtonBgGradientView.alpha = value
            // reveal balance title, QR code button and balance gem image
            balanceTitleTextView.alpha = value
            userWalletInfoButton.alpha = value
            balanceGemImageView.alpha = value
        }
        sendTariButtonIsVisible = true
        anim.duration = Constants.UI.Home.startupAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        anim.start()
    }

    /**
     * Play onboarding animation.
     */
    private fun playOnboardingAnim() {
        onboardingContentView.visibility = View.VISIBLE
        scrollView.translationY = scrollView.height.toFloat()
        swipeRefreshLayout.isEnabled = false
        hideSendTariButtonAnimated()

        updateBalanceInfoData()
        // initialize the balance view controller
        balanceViewController =
            BalanceViewController(
                this,
                balanceDigitContainerView,
                balanceDecimalDigitContainerView,
                balanceInfo // initial value
            )

        scrollView.scrollTo(0, scrollView.height)
        scrollView.translationY = scrollView.height.toFloat()
        scrollContentView.alpha = 1f
        // scroll view translation animation
        val scrollViewTransAnim =
            ObjectAnimator.ofFloat(
                scrollView,
                View.TRANSLATION_Y,
                scrollView.height.toFloat(),
                0f
            )
        // background fade animation
        val blackBgViewFadeAnim = ValueAnimator.ofFloat(0f, 1f)
        blackBgViewFadeAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            txListBgOverlayView.alpha = value
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
                wr.get()?.topContentContainerView?.visibility = View.VISIBLE
                wr.get()?.balanceTitleTextView?.alpha = 1f
                wr.get()?.userWalletInfoButton?.alpha = 1f
                wr.get()?.balanceGemImageView?.alpha = 1f
            }
        )
        animSet.start()
    }

    private fun showNoTxsTextView() {
        noTxsInfoTextView.alpha = 0f
        noTxsInfoTextView.visibility = View.VISIBLE
        val anim = ObjectAnimator.ofFloat(noTxsInfoTextView, "alpha", 0f, 1f)
        anim.duration = Constants.UI.mediumDurationMs
        anim.start()
    }

    private fun requestTestnetTari() {
        testnetTariRequestIsInProgress = true
        val error = WalletError()
        walletService!!.requestTestnetTari(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
    }

    private fun testnetTariRequestSuccessful(senderPublicKey: PublicKey) {
        updateAllDataAndUI(restartBalanceUI = false)
        // display dialog
        uiHandler.postDelayed({
            showTestnetTariReceivedDialog(senderPublicKey)
        }, Constants.UI.Home.showTariBotDialogDelayMs)
        testnetTariRequestIsInProgress = false
    }

    private fun testnetTariRequestError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        testnetTariRequestIsInProgress = false
        showSendTariButtonAnimated()
    }

    private fun showTestnetTariReceivedDialog(senderPublicKey: PublicKey) {
        Dialog(this, R.style.Theme_AppCompat_Dialog).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.home_dialog_testnet_tari_received)
            setCancelable(false)
            window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            findViewById<TextView>(R.id.home_tari_bot_dialog_txt_try_later)
                .setOnClickListener {
                    dismiss()
                    rootView.postDelayed({
                        showSendTariButtonAnimated()
                    }, Constants.UI.shortDurationMs)
                }
            findViewById<TextView>(R.id.home_tari_bot_dialog_btn_send_tari)
                .setOnClickListener {
                    dismiss()
                    sendTariToTestnetSender(senderPublicKey)
                    rootView.postDelayed({
                        showSendTariButtonAnimated()
                    }, Constants.UI.longDurationMs)

                }
            window?.setGravity(Gravity.BOTTOM)
            show()
        }
    }

    private fun sendTariToTestnetSender(senderPublicKey: PublicKey) {
        val intent = Intent(this@HomeActivity, SendTariActivity::class.java)
        // attach contact
        val error = WalletError()
        val contacts = walletService!!.getContacts(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }
        for (contact in contacts) {
            if (senderPublicKey.hexString == contact.publicKey.hexString) {
                intent.putExtra("recipientUser", contact)
                break
            }
        }

        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    /**
     * Opens user wallet info on button click.
     */
    @OnClick(R.id.home_img_wallet_info)
    fun walletInfoImageClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val intent = Intent(this@HomeActivity, WalletInfoActivity::class.java)
        startActivity(intent)
    }

    @OnClick(R.id.home_btn_send_tari)
    fun sendTariButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        animateSendTariButtonOnClick(
            Constants.UI.Button.clickScaleAnimFullScale,
            Constants.UI.Button.clickScaleAnimSmallScale
        )
    }

    private fun endOnboarding() {
        isOnboarding = false
        onboardingContentView.visibility = View.GONE
        txListHeaderView.visibility = View.VISIBLE
        swipeRefreshLayout.isEnabled = true
        updateAllDataAndUI(restartBalanceUI = false)
        // request Testnet Tari if no txs
        if (txListIsEmpty) {
            handler.postDelayed({
                wr.get()?.requestTestnetTari()
            }, Constants.UI.xxLongDurationMs)

        } else {
            showSendTariButtonAnimated()
        }
    }

    /**
     * Called when a tx is sent successfully.
     */
    private fun onSendTxSuccessful() {
        scrollView.scrollToTop()
        recyclerView.scrollToPosition(0)
        val topMargin = rootView.height - txListContainerMinimizedTopMargin
        scrollContentView.y = topMargin.toFloat()
        val anim = ValueAnimator.ofInt(
            topMargin,
            0
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Int
            scrollContentView.y = value.toFloat()
        }
        anim.duration = Constants.UI.longDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()

        // update data
        AsyncTask.execute {
            updateAllDataAndUI(restartBalanceUI = true)
        }
    }

    /**
     * Called when a tx gets clicked.
     */
    override fun onTxSelected(tx: Tx) {
        Logger.i("Transaction with id ${tx.id} selected.")
        startActivity(TxDetailActivity.createIntent(this, tx))
    }

    // region send tari button animation listener

    override fun onAnimationStart(animation: Animation?) {
        // no-op
    }

    override fun onAnimationRepeat(animation: Animation?) {
        // no-op
    }

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

    @OnClick(R.id.home_btn_close_tx_list)
    fun minimizeListButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        scrollView.smoothScrollTo(0, 0)
        recyclerView.smoothScrollToPosition(0)
    }

    @OnClick(R.id.home_vw_grabber_container)
    fun grabberContainerViewClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        scrollView.smoothScrollTo(
            0,
            scrollContentView.height - scrollView.height
        )
    }

    @OnLongClick(R.id.home_vw_grabber_container)
    fun grabberContainerViewLongClicked() {
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
        sendTariButton.alpha = 1f
        val initialMargin = UiUtil.getBottomMargin(sendTariButton)
        val marginDelta = sendTariButtonVisibleBottomMargin - UiUtil.getBottomMargin(sendTariButton)
        val anim = ValueAnimator.ofFloat(
            0f,
            1f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                sendTariButton,
                (initialMargin + marginDelta * value).toInt()
            )
            sendTariButtonBgGradientView.alpha = value
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
        val initialMargin = UiUtil.getBottomMargin(sendTariButton)
        val marginDelta = sendTariButtonHiddenBottomMargin - UiUtil.getBottomMargin(sendTariButton)
        val anim = ValueAnimator.ofFloat(
            0f,
            1f
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                sendTariButton,
                (initialMargin + marginDelta * value).toInt()
            )
            sendTariButtonBgGradientView.alpha = 1f - value
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
        sendTariButton.startAnimation(anim)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view == null || event == null) {
            return false
        }
        if (view === scrollBgEnabler) {
            scrollView.requestDisallowInterceptTouchEvent(true)
            // profile button - handle touch
            val rect = Rect()
            userWalletInfoButton.getGlobalVisibleRect(rect)
            if (rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                userWalletInfoButton.dispatchTouchEvent(event)
            }
            // event consumed
            return true
        }
        if (view === scrollView || view === recyclerView) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                }
                MotionEvent.ACTION_UP -> {
                    if (isOnboarding && scrollView.scrollY == 0) {
                        endOnboarding()
                    }
                    scrollView.flingIsRunning = false
                    scrollView.postDelayed({ scrollView.completeScroll() }, 50L)
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
            val maxScroll = scrollView.getChildAt(0).height - scrollView.height
            val ratio = scrollView.scrollY.toFloat() / maxScroll.toFloat()
            onboardingContentView.alpha = ratio
            txListBgOverlayView.alpha = ratio
            UiUtil.setTopMargin(
                topContentContainerView,
                topContentContainerViewTopMargin
                        + (ratio * topContentContainerViewScrollVerticalShift).toInt()
            )
            txListTitleTextView.alpha = ratio
            minimizeTxListButton.alpha = ratio

            if (!isOnboarding) {
                UiUtil.setTopMargin(
                    txListHeaderView,
                    ((ratio - 1) * txListHeaderHeight).toInt()
                )
                grabberView.alpha = max(0f, 1f - ratio * grabberViewAlphaScrollAnimCoefficient)

                UiUtil.setWidth(
                    grabberView,
                    (max(
                        0f,
                        1f - ratio * grabberViewWidthScrollAnimCoefficient
                    ) * grabberViewWidth).toInt()
                )
                val grabberBgDrawable = grabberContainerView.background as GradientDrawable
                grabberBgDrawable.cornerRadius = max(
                    0f,
                    1f - ratio * grabberViewCornerRadiusScrollAnimCoefficient
                ) * grabberCornerRadius
            } else if (ratio == 0f && !isDragging) {
                endOnboarding()
            }
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
        headerElevationView.alpha = min(
            Constants.UI.scrollDepthShadowViewMaxOpacity,
            totalDeltaY / listItemHeight.toFloat()
        )
    }

    class ScrollListener(activity: HomeActivity) : RecyclerView.OnScrollListener() {

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
}