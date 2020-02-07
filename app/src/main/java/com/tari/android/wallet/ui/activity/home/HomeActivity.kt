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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindColor
import butterknife.BindDimen
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnLongClick
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.activity.EXTRA_QR_DATA
import com.tari.android.wallet.ui.activity.QRScannerActivity
import com.tari.android.wallet.ui.activity.home.adapter.TxListAdapter
import com.tari.android.wallet.ui.activity.log.DebugLogActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.math.max

/**
 * Home activity - transaction list.
 *
 * @author The Tari Development Team
 */

private const val REQUEST_QR_SCANNER = 101

class HomeActivity : BaseActivity(),
    ServiceConnection,
    SwipeRefreshLayout.OnRefreshListener,
    View.OnScrollChangeListener,
    View.OnTouchListener,
    Animation.AnimationListener,
    TxListAdapter.Listener {

    @BindView(R.id.home_vw_top_content_container)
    lateinit var topContentContainerView: View

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
    @BindView(R.id.home_v_scroll_depth_gradient)
    lateinit var scrollDepthView: View

    @BindView(R.id.home_btn_send_tari)
    lateinit var sendTariButton: Button
    @BindView(R.id.home_vw_send_tari_btn_bg_gradient)
    lateinit var sendTariButtonBgGradientView: View

    // Balance views.
    @BindView(R.id.home_txt_available_balance)
    lateinit var balanceTitleTextView: TextView
    @BindView(R.id.home_img_balance_gem)
    lateinit var balanceGemImageView: ImageView
    @BindView(R.id.home_img_btn_qr)
    lateinit var qrCodeButton: ImageButton

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

    @BindView(R.id.home_vw_test_data_blocker)
    lateinit var testDataBlockerView: View
    @BindView(R.id.home_prog_bar_test_data)
    lateinit var testDataProgressBar: ProgressBar
    @BindView(R.id.home_txt_test_data_warning)
    lateinit var testDataWarningTextView: TextView

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

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    // tx list
    private lateinit var recyclerViewAdapter: TxListAdapter
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private val completedTxs = mutableListOf<CompletedTx>()
    private val pendingInboundTxs = mutableListOf<PendingInboundTx>()
    private val pendingOutboundTxs = mutableListOf<PendingOutboundTx>()

    // balance controller
    private lateinit var balanceViewController: BalanceViewController

    // grabber drag animation constants
    private var grabberViewAlphaScrollAnimCoefficient = 1.2f
    private var grabberViewWidthScrollAnimCoefficient = 1.1f
    private var grabberViewCornerRadiusScrollAnimCoefficient = 1.4f

    private var sendTariButtonIsVisible = true

    private var walletService: TariWalletService? = null
    private val wr = WeakReference(this)

    override val contentViewId = R.layout.activity_home

    /**
     * This listener is used only to animate the visibility of the scroll depth gradient view.
     */
    private var scrollListener = ScrollListener(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // makeStatusBarTransparent() -- commented out to fix the UI cutout issue
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_vw_root)) { _, insets ->
            insets.consumeSystemWindowInsets()
        }

        // hide background overlay
        txListBgOverlayView.alpha = 0f

        // hide tx list header
        UiUtil.setTopMargin(
            txListHeaderView,
            -txListHeaderHeight
        )

        // initialize pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this)
        // configure the refreshing colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.home_bg_gradient_start,
            R.color.home_bg_gradient_center,
            R.color.home_bg_gradient_end
        )

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

        scrollBgEnabler.setOnTouchListener(this)
        scrollView.setOnTouchListener(this)
        recyclerView.setOnTouchListener(this)

        scrollView.post {
            wr.get()?.setScrollViewContentHeight()
        }

        scrollDepthView.alpha = 0f

        UiUtil.setProgressBarColor(testDataProgressBar, whiteColor)
        sendTariButton.visibility = View.INVISIBLE
        sendTariButtonBgGradientView.alpha = 0f
    }

    override fun onStart() {
        super.onStart()
        // start service if not started yet
        if (walletService == null) {
            // start the wallet service
            ContextCompat.startForegroundService(
                this,
                Intent(this, WalletService::class.java)
            )
            // bind to service
            val bindIntent = Intent(this@HomeActivity, WalletService::class.java)
            //intent.action = TariWalletService::class.java.name
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service.")
        walletService = TariWalletService.Stub.asInterface(service)
        AsyncTask.execute {
            wr.get()?.walletService?.generateTestData()
            wr.get()?.initializeData()
        }
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        walletService = null
    }

    private fun setScrollViewContentHeight() {
        val recyclerViewHeight = testDataBlockerView.height - txListHeaderHeight
        val contentHeight =
            txListContainerMinimizedTopMargin + grabberContainerHeight + recyclerViewHeight
        UiUtil.setHeight(swipeRefreshLayout, recyclerViewHeight)
        UiUtil.setHeight(scrollContentView, contentHeight)
        scrollView.scrollTo(0, 0)

        scrollView.setOnScrollChangeListener(this)
        recyclerView.setOnScrollChangeListener(this)
        recyclerView.addOnScrollListener(scrollListener)
    }

    /**
     * Called after the service is connected.
     */
    private fun initializeData() {
        // display txs
        completedTxs.clear()
        completedTxs.addAll(walletService!!.completedTxs)
        pendingInboundTxs.clear()
        pendingInboundTxs.addAll(walletService!!.pendingInboundTxs)
        pendingOutboundTxs.clear()
        pendingOutboundTxs.addAll(walletService!!.pendingOutboundTxs)
        val balanceInfo = walletService!!.balanceInfo
        val wr = WeakReference<HomeActivity>(this)
        scrollView.post {
            wr.get()?.recyclerViewAdapter?.notifyDataChanged()
            wr.get()?.runStartupAnimation(balanceInfo)
        }
    }

    /**
     * Called on refresh.
     */
    private fun updateData() {
        // balance
        val balanceInfo = walletService!!.balanceInfo
        // txs
        completedTxs.clear()
        completedTxs.addAll(walletService!!.completedTxs)
        pendingInboundTxs.clear()
        pendingInboundTxs.addAll(walletService!!.pendingInboundTxs)
        pendingOutboundTxs.clear()
        pendingOutboundTxs.addAll(walletService!!.pendingOutboundTxs)
        val wr = WeakReference<HomeActivity>(this)
        scrollView.post {
            balanceViewController.balance = balanceInfo.availableBalance.tariValue
            wr.get()?.recyclerViewAdapter?.notifyDataChanged()
            wr.get()?.swipeRefreshLayout?.isRefreshing = false
            wr.get()?.recyclerView?.isNestedScrollingEnabled = true
            wr.get()?.scrollListener?.reset()
        }
    }

    /**
     * The startup animation - reveals the list, balance and other views.
     */
    private fun runStartupAnimation(balanceInfo: BalanceInfo) {
        testDataBlockerView.visibility = View.GONE
        sendTariButton.visibility = View.VISIBLE

        // initialize the balance view controller
        balanceViewController =
            BalanceViewController(
                this,
                balanceDigitContainerView,
                balanceDecimalDigitContainerView,
                balanceInfo.availableBalance.tariValue // initial value
            )

        // show digits
        balanceViewController.runStartupAnimation()

        // show button and list
        val sendTariButtonMarginDelta =
            sendTariButtonVisibleBottomMargin - sendTariButtonInitialBottomMargin
        // animator runs from 0.0 (begin) to 1.0 (completion)

        val listAnim = ValueAnimator.ofFloat(
            0.0f,
            1.0f
        )
        listAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            // value will run from 0.0 to 1.0
            val value = valueAnimator.animatedValue as Float
            // animate the list (will move upwards)
            scrollView.y = scrollViewStartupAnimHeight * (1 - value)
            // animate the send tari button (will move upwards)
            UiUtil.setBottomMargin(
                sendTariButton,
                (sendTariButtonInitialBottomMargin + value * sendTariButtonMarginDelta).toInt()
            )
            sendTariButtonBgGradientView.alpha = value
            // reveal balance title, QR code button and balance gem image
            balanceTitleTextView.alpha = value
            qrCodeButton.alpha = value
            balanceGemImageView.alpha = value
        }
        listAnim.duration = Constants.UI.Home.startupAnimDurationMs
        listAnim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        listAnim.start()
    }

    /**
     * Called on swipe refresh.
     */
    override fun onRefresh() {
        recyclerView.isNestedScrollingEnabled = false
        AsyncTask.execute {
            wr.get()?.updateData()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null
        recyclerView.setOnTouchListener(null)
        unbindService(this)
        EventBus.unsubscribe(this)
        super.onDestroy()
    }

    /**
     * Opens QR code scanner on button click.
     */
    @OnClick(R.id.home_img_btn_qr)
    fun onQRCodeScanClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val intent = Intent(this, QRScannerActivity::class.java)
        startActivityForResult(intent, REQUEST_QR_SCANNER)
        overridePendingTransition(R.anim.slide_up, 0)
    }

    @OnClick(R.id.home_btn_send_tari)
    fun sendTariButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        animateSendTariButtonClick(
            Constants.UI.Button.clickScaleAnimFullScale,
            Constants.UI.Button.clickScaleAnimSmallScale
        )
    }

    /**
     * Called when a tx gets clicked.
     */
    override fun onTxSelected(tx: Tx) {
        Logger.i("Transaction with id ${tx.id} selected.")
    }

    override fun onAnimationStart(animation: Animation?) {
        // no-op
    }

    override fun onAnimationRepeat(animation: Animation?) {
        // no-op
    }

    override fun onAnimationEnd(animation: Animation?) {
        if (sendTariButtonClickAnimIsRunning) { // bounce back the button
            animateSendTariButtonClick(
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
    fun grabberContainerViewLongClicked()
    {
        val intent = Intent(this@HomeActivity, DebugLogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("log", wr?.get()?.walletService?.logFile);
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val text = data.getStringExtra(EXTRA_QR_DATA)
            Toast.makeText(this, "Scan result: $text", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Reveals the send tari button with animation after a specified delay time in ms.
     */
    private fun showSendTariButton() {
        if (sendTariButtonIsVisible) {
            return
        }
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
        anim.duration = Constants.UI.mediumAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()
        sendTariButtonIsVisible = true
    }

    /**
     * Hides the send tari button with animation after a specified delay time in ms.
     */
    private fun hideSendTariButton() {
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
        anim.duration = Constants.UI.mediumAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()
        sendTariButtonIsVisible = false
    }

    private var sendTariButtonClickAnimIsRunning = false

    private fun animateSendTariButtonClick(startScale: Float, endScale: Float) {
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
            // QR code button - handle touch
            val rect = Rect()
            qrCodeButton.getGlobalVisibleRect(rect)
            if (rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                qrCodeButton.dispatchTouchEvent(event)
            }
            // event consumed
            return true
        }
        if (view === scrollView) {
            if (event.action == MotionEvent.ACTION_UP) {
                scrollView.flingIsRunning = false
                scrollView.postDelayed({ scrollView.completeScroll() }, 50L)
            }
            return false
        }
        if (view === recyclerView) {
            if (event.action == MotionEvent.ACTION_UP) {
                scrollView.flingIsRunning = false
                scrollView.postDelayed({ scrollView.completeScroll() }, 50L)
            }
            return false
        }
        return false
    }

    /**
     * On-scroll UI changes.
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
            txListBgOverlayView.alpha = ratio
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

            UiUtil.setTopMargin(
                topContentContainerView,
                topContentContainerViewTopMargin
                        + (ratio * topContentContainerViewScrollVerticalShift).toInt()
            )

            txListTitleTextView.alpha = ratio
            minimizeTxListButton.alpha = ratio
        }
        if (scrollY > oldScrollY) {
            hideSendTariButton()
        } else if (scrollY < oldScrollY) {
            showSendTariButton()
        }
    }

    // region scroll depth gradient view controls

    fun onRecyclerViewScrolled(totalDeltaY: Int) {
        scrollDepthView.alpha = min(
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
