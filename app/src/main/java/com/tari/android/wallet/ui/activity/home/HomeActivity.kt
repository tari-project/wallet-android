/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.BindDimen
import butterknife.OnClick
import butterknife.ButterKnife
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.makeStatusBarTransparent
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.*
import java.lang.Float.max
import java.lang.ref.WeakReference

/**
 * Home - transaction list.
 *
 * @author The Tari Development Team
 */
class HomeActivity : AppCompatActivity(),
    SwipeRefreshLayout.OnRefreshListener,
    View.OnScrollChangeListener,
    View.OnTouchListener {

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

    @BindView(R.id.home_btn_send_tari)
    lateinit var sendTariButton: Button

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

    @BindDimen(R.dimen.home_top_content_container_view_top_margin)
    @JvmField
    var topContentContainerViewTopMargin = 0
    @BindDimen(R.dimen.home_top_content_container_scroll_vertical_shift)
    @JvmField
    var topContentContainerViewScrollVerticalShift = 0

    @BindDimen(R.dimen.home_tx_list_header_height)
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

    // tx list
    private lateinit var recyclerViewAdapter: RecyclerView.Adapter<*>
    private lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    private val txList: List<DummyTx> = dummyTransactions.toList()

    // balance controller
    private lateinit var balanceViewController: BalanceViewController

    //
    private var grabberViewAlphaScrollAnimCoefficient = 1.2f
    private var grabberViewWidthScrollAnimCoefficient = 1.1f
    private var grabberViewCornerRadiusScrollAnimCoefficient = 1.4f

    private var sendTariButtonIsVisible = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        //makeStatusBarTransparent() -- commented out to fix the UI cutout issue
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_vw_root)) { _, insets ->
            insets.consumeSystemWindowInsets()
        }

        ButterKnife.bind(this)

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
            TxListRecyclerViewAdapter(
                txList
            )
        recyclerView.adapter = recyclerViewAdapter
        // recycler view is initially disabled
        recyclerView.isClickable = false

        // initialize the balance view controller
        balanceViewController =
            BalanceViewController(
                this,
                balanceDigitContainerView,
                balanceDecimalDigitContainerView,
                dummyWallet.balanceTaris // initial value
            )

        scrollBgEnabler.setOnTouchListener(this)
        scrollView.setOnTouchListener(this)
        recyclerView.setOnTouchListener(this)

        scrollView.post {
            setScrollViewContentHeight()
            runStartupAnimation()
        }
    }

    private fun setScrollViewContentHeight() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val recyclerViewHeight = screenHeight - txListHeaderHeight
        val contentHeight =
            txListContainerMinimizedTopMargin + grabberContainerHeight + recyclerViewHeight
        UiUtil.setHeight(swipeRefreshLayout, recyclerViewHeight)
        UiUtil.setHeight(scrollContentView, contentHeight)
        scrollView.scrollTo(0, 0)

        scrollView.setOnScrollChangeListener(this)
        recyclerView.setOnScrollChangeListener(this)
    }

    /**
     * The startup animation - reveals the list, balance and other views.
     */
    private fun runStartupAnimation() {
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
            // reveal balance title, QR code button and balance gem image
            balanceTitleTextView.alpha = value
            qrCodeButton.alpha = value
            balanceGemImageView.alpha = value
        }
        listAnim.duration = Constants.UI.Home.startupAnimDuration
        listAnim.interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
        listAnim.start()
    }

    /**
     * Called on swipe refresh.
     */
    override fun onRefresh() {
        val wr = WeakReference(this)
        recyclerView.isNestedScrollingEnabled = false
        // end refresh after some duration - for test purposes
        swipeRefreshLayout.postDelayed(
            {
                wr.get()?.swipeRefreshLayout?.isRefreshing = false
                wr.get()?.recyclerView?.isNestedScrollingEnabled = true
            },
            Constants.UI.Home.swipeRefreshDummyDuration
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null
        recyclerView.setOnTouchListener(null)
        super.onDestroy()
    }

    /**
     * For test purposes only - update balance when the QR code button is clicked.
     */
    @OnClick(R.id.home_img_btn_qr)
    fun changeBalance(view: View) {
        UiUtil.temporarilyDisableClick(view)
        // get next balance
        val balance = nextBalance()
        dummyWallet.balanceTaris = balance
        // set balance
        balanceViewController.balance = dummyWallet.balanceTaris
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

    /**
     * Reveals the send tari button with animation after a specified delay time in ms.
     */
    private fun showSendTariButton() {
        if (sendTariButtonIsVisible) {
            return
        }
        val anim = ValueAnimator.ofInt(
            UiUtil.getBottomMargin(sendTariButton),
            sendTariButtonVisibleBottomMargin
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            UiUtil.setBottomMargin(
                sendTariButton,
                valueAnimator.animatedValue as Int
            )
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
        val anim = ValueAnimator.ofInt(
            UiUtil.getBottomMargin(sendTariButton),
            sendTariButtonHiddenBottomMargin
        )
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            UiUtil.setBottomMargin(
                sendTariButton,
                valueAnimator.animatedValue as Int
            )
        }
        anim.duration = Constants.UI.mediumAnimDurationMs
        anim.interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
        anim.start()
        sendTariButtonIsVisible = false
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

}
