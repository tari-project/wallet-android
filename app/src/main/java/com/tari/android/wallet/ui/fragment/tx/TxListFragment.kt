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
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.os.postDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.squareup.seismic.ShakeDetector
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.databinding.FragmentTxListBinding
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.activity.debug.DebugActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.dialog.backup.BackupWalletDialog
import com.tari.android.wallet.ui.dialog.testnet.TestnetReceivedDialog
import com.tari.android.wallet.ui.dialog.ttl.TtlStoreWalletDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListAdapter
import com.tari.android.wallet.ui.fragment.tx.questionMark.QuestionMarkViewModel
import com.tari.android.wallet.ui.fragment.tx.ui.BalanceViewController
import com.tari.android.wallet.ui.fragment.tx.ui.CustomScrollView
import com.tari.android.wallet.ui.fragment.tx.ui.UpdateProgressViewController
import com.tari.android.wallet.ui.resource.AnimationResource
import com.tari.android.wallet.ui.resource.ResourceContainer
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

internal class TxListFragment : CommonFragment<FragmentTxListBinding, TxListViewModel>(),
    View.OnScrollChangeListener,
    View.OnTouchListener,
    CustomScrollView.Listener,
    UpdateProgressViewController.Listener,
    ShakeDetector.Listener {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var tracker: Tracker

    private val networkIndicatorViewModel: ConnectionIndicatorViewModel by viewModels()
    private val questionMarkViewModel: QuestionMarkViewModel by viewModels()

    private val handler: Handler = Handler(Looper.getMainLooper())

    // This listener is used only to animate the visibility of the scroll depth gradient view.
    private val recyclerViewScrollListener = RecyclerViewScrollListener(::onRecyclerViewScrolled)
    private val shakeDetector = ShakeDetector(this)
    private var recyclerViewAdapter: TxListAdapter = TxListAdapter()
    private lateinit var balanceViewController: BalanceViewController
    private lateinit var updateProgressViewController: UpdateProgressViewController
    private val container = ResourceContainer()
    private var isOnboarding = false
    private var isInDraggingSession = false

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
        val viewModel: TxListViewModel by viewModels()
        bindViewModel(viewModel)
        setupUI()
        subscribeToEventBus()
        subscribeToViewModel()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(connected) { initializeTxListUI() }

        observe(refreshBalanceInfo) { updateBalanceInfoUI(it) }

        observe(navigation) { processNavigation(it) }

        observe(showTtlStoreDialog) { replaceDialog(TtlStoreWalletDialog(requireContext(), it)) }

        observe(showBackupPrompt) { replaceDialog(BackupWalletDialog(requireContext(), it)) }

        observe(showTestnetReceived) { replaceDialog(TestnetReceivedDialog(requireContext(), it)) }

        observe(txSendSuccessful) { playTxSendSuccessfulAnim() }

        observe(list) { updateTxListUI(it) }

        observeOnLoad(requiredConfirmationCount)
        observeOnLoad(balanceInfo)
        observeOnLoad(listUpdateTrigger)
        observeOnLoad(debouncedList)
    }

    override fun onStart() {
        (requireContext().getSystemService(AppCompatActivity.SENSOR_SERVICE) as? SensorManager)?.let(shakeDetector::start)
        super.onStart()
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        shakeDetector.stop()
        super.onStop()
    }

    override fun onDestroyView() {
        EventBus.unsubscribe(this)
        EventBus.networkConnectionState.unsubscribe(this)
        if (::updateProgressViewController.isInitialized) {
            updateProgressViewController.destroy()
        }
        container.dispose()
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() = with(ui) {
        txListHeaderView.setTopMargin(-dimenPx(R.dimen.common_header_height))
        updateProgressViewController =
            UpdateProgressViewController(
                ui.updateProgressContentView,
                this@TxListFragment
            )
        viewModel.progressControllerState = updateProgressViewController.state
        scrollView.bindUI()
        scrollView.listenerWeakReference = WeakReference(this@TxListFragment)
        scrollView.updateProgressViewController = updateProgressViewController
        setupRecyclerView()
        scrollBgEnablerView.setOnTouchListener(this@TxListFragment)
        scrollView.setOnTouchListener(this@TxListFragment)
        txRecyclerView.setOnTouchListener(this@TxListFragment)

        headerElevationView.alpha = 0F
        balanceTextView.alpha = 0F
        availableBalanceTextView.alpha = 0F
        balanceQuestionMark.alpha = 0F
        availableBalance.alpha = 0F
        networkStatusStateIndicatorView.alpha = 0F
        balanceGemImageView.alpha = 0F
        scrollContentView.alpha = 0F
        noTxsInfoTextView.gone()
        onboardingContentView.gone()
        topContentContainerView.invisible()
        setupCTAs()
    }

    private fun setupCTAs() = with(ui) {
        scrollView.post { setupScrollView() }
        closeTxListButton.setOnClickListener(::minimizeListButtonClicked)
        grabberContainerView.setOnClickListener(::grabberContainerViewClicked)
        grabberContainerView.setOnLongClickListener {
            grabberContainerViewLongClicked()
            true
        }
    }

    private fun setupRecyclerView() {
        ui.txRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.processItemClick(it) })
        ui.txRecyclerView.adapter = recyclerViewAdapter
        ui.txRecyclerView.isVerticalScrollBarEnabled = false
    }

    private fun setupScrollView() = with(ui) {
        val recyclerViewHeight = ui.rootView.height - dimenPx(R.dimen.common_header_height)
        val contentHeight =
            dimenPx(R.dimen.home_tx_list_container_minimized_top_margin) + dimenPx(R.dimen.home_grabber_container_height) + recyclerViewHeight
        recyclerViewContainerView.setLayoutHeight(recyclerViewHeight)
        scrollContentView.setLayoutHeight(contentHeight)
        scrollView.recyclerViewContainerInitialHeight = recyclerViewHeight
        scrollView.scrollToTop()

        scrollView.setOnScrollChangeListener(this@TxListFragment)
        txRecyclerView.setOnScrollChangeListener(this@TxListFragment)
        txRecyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.App.AppForegrounded>(this) {
            if (viewModel.serviceConnection.currentState.service != null && updateProgressViewController.state.state == UpdateProgressViewController.State.IDLE
            ) {
                lifecycleScope.launch(Dispatchers.Main) {
                    updateProgressViewController.reset()
                    updateProgressViewController.start(viewModel.walletService)
                    ui.scrollView.beginUpdate()
                }
            }
        }
    }

    private fun subscribeToViewModel() {
        ui.networkStatusStateIndicatorView.viewLifecycle = viewLifecycleOwner
        ui.networkStatusStateIndicatorView.bindViewModel(networkIndicatorViewModel)
        ui.balanceQuestionMark.viewLifecycle = viewLifecycleOwner
        ui.balanceQuestionMark.bindViewModel(questionMarkViewModel)
    }

    private fun processNavigation(navigation: TxListNavigation) {
        val router = requireActivity() as TxListRouter
        when (navigation) {
            TxListNavigation.ToTTLStore -> router.toTTLStore()
            is TxListNavigation.ToTxDetails -> router.toTxDetails(navigation.tx)
            is TxListNavigation.ToSendTariToUser -> navigateToSendTari(navigation.user)
            TxListNavigation.ToAllSettings -> router.toAllSettings()
        }
    }

    private fun navigateToSendTari(user: User) {
        val intent = Intent(requireContext(), SendTariActivity::class.java)
        intent.putExtra("recipientUser", user)
        val parameters: Map<String, String> = emptyMap()
        parameters[DeepLink.PARAMETER_NOTE]?.let { intent.putExtra(DeepLink.PARAMETER_NOTE, it) }
        parameters[DeepLink.PARAMETER_AMOUNT]?.toDoubleOrNull()?.let { intent.putExtra(DeepLink.PARAMETER_AMOUNT, it) }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun updateTxListUI(list: MutableList<CommonViewHolderItem>) {
        recyclerViewAdapter.update(list)
        if (list.isEmpty()) {
            showNoTxsTextView()
        } else {
            ui.noTxsInfoTextView.gone()
        }
    }

    private fun updateBalanceInfoUI(restart: Boolean) {
        val balanceInfo = viewModel.balanceInfo.value!!

        val availableBalance = WalletUtil.balanceFormatter.format(balanceInfo.availableBalance.tariValue)
        ui.availableBalance.text = availableBalance

        if (restart) {
            ui.balanceDigitContainerView.removeAllViews()
            ui.balanceDecimalsDigitContainerView.removeAllViews()
            createBalanceInfoController(balanceInfo)
            // show digits
            balanceViewController.runStartupAnimation()
        } else {
            if (::balanceViewController.isInitialized) {
                balanceViewController.balanceInfo = balanceInfo
            } else {
                createBalanceInfoController(balanceInfo)
            }
        }
    }

    private fun createBalanceInfoController(info: BalanceInfo) {
        balanceViewController = BalanceViewController(requireContext(), ui.balanceDigitContainerView, ui.balanceDecimalsDigitContainerView, info)
    }


    private fun initializeTxListUI() {
        if (sharedPrefsWrapper.onboardingDisplayedAtHome) {
            recyclerViewAdapter.update(viewModel.list.value!!)
            playNonOnboardingStartupAnim()
            if (viewModel.txListIsEmpty) {
                showNoTxsTextView()
            }
            if (!sharedPrefsWrapper.faucetTestnetTariRequestCompleted && !sharedPrefsWrapper.isRestoredWallet
            ) {
                viewModel.requestTestnetTari()
            }
            updateProgressViewController.reset()
            ui.scrollView.beginUpdate()
            updateProgressViewController.start(viewModel.walletService)
        } else {
            isOnboarding = true
            playOnboardingAnim()
            sharedPrefsWrapper.onboardingDisplayedAtHome = true
        }
    }

    private fun showNoTxsTextView() {
        if (isOnboarding) return

        ui.noTxsInfoTextView.alpha = 0F
        ui.noTxsInfoTextView.visible()
        ValueAnimator.ofFloat(0F, 1F).apply {
            duration = Constants.UI.mediumDurationMs
            addUpdateListener { ui.noTxsInfoTextView.alpha = it.animatedValue as Float }
        }.start()
    }

    private fun minimizeListButtonClicked(view: View) {
        view.temporarilyDisableClick()
        ui.scrollView.smoothScrollTo(0, 0)
        ui.txRecyclerView.smoothScrollToPosition(0)
    }

    private fun grabberContainerViewClicked(view: View) {
        view.temporarilyDisableClick()
        ui.scrollView.smoothScrollTo(0, ui.scrollContentView.height - ui.scrollView.height)
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
        updateProgressViewController.start(viewModel.walletService)
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
                        viewModel.displayNetworkConnectionErrorDialog()
                    }
                    UpdateProgressViewController.FailureReason.BASE_NODE_VALIDATION_ERROR -> {
                        viewModel.displayNetworkConnectionErrorDialog()
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

        viewModel.refreshAllData()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        ui.networkStatusStateIndicatorView.dispatchTouchEvent(event)
        ui.balanceQuestionMark.dispatchTouchEvent(event)

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
                ui.txListHeaderView.setTopMargin(((ratio - 1) * dimenPx(R.dimen.common_header_height)).toInt())
                ui.grabberView.alpha = max(0F, 1F - ratio * GRABBER_ALPHA_SCROLL_COEFFICIENT)
                val width = (max(0F, 1F - ratio * GRABBER_WIDTH_SCROLL_COEFFICIENT) * dimenPx(R.dimen.home_grabber_width)).toInt()
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

    private fun playTxSendSuccessfulAnim() {
        ui.scrollView.scrollToTop()
        ui.txRecyclerView.scrollToPosition(0)
        val topMargin =
            ui.rootView.height - dimenPx(R.dimen.home_tx_list_container_minimized_top_margin)
        ui.scrollContentView.y = topMargin.toFloat()
        ValueAnimator.ofInt(topMargin, 0).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Int
                ui.scrollContentView.y = value.toFloat()
            }
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.EASE_OUT_EXPO)
            start()
        }
    }

    /**
     * The startup animation - reveals the list, balance and other views.
     */
    private fun playNonOnboardingStartupAnim() {
        ui.topContentContainerView.visible()
        updateBalanceInfoUI(true)
        ui.scrollView.scrollToTop()
        handler.postDelayed(Constants.UI.Home.startupAnimDurationMs) { ui.blockerView.gone() }
        ValueAnimator.ofFloat(0.0F, 1.0F).apply {
            duration = Constants.UI.Home.startupAnimDurationMs
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                // animate the list (will move upwards)
                ui.scrollContentView.y = dimenPx(R.dimen.home_scroll_view_startup_anim_height) * (1 - value)
                ui.scrollContentView.alpha = value
                ui.balanceTextView.alpha = 1F
                ui.availableBalanceTextView.alpha = 1F
                ui.balanceQuestionMark.alpha = 1F
                ui.availableBalance.alpha = 1F
                ui.networkStatusStateIndicatorView.alpha = 1F
                ui.balanceGemImageView.alpha = 1F
            }
        }.also { AnimationResource(it).attachAndCutoffOnFinish(container) }
            .start()
    }

    /**
     * Play onboarding animation.
     */
    private fun playOnboardingAnim() {
        ui.onboardingContentView.visible()
        ui.noTxsInfoTextView.gone()
        // initialize the balance view controller

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
        AnimatorSet().apply {
            playTogether(scrollViewTransAnim, blackBgViewFadeAnim)
            duration = Constants.UI.Home.welcomeAnimationDurationMs
            addListener(
                onEnd = {
                    ui.topContentContainerView.visible()
                    ui.balanceTextView.alpha = 1F
                    ui.availableBalanceTextView.alpha = 1F
                    ui.balanceQuestionMark.alpha = 1F
                    ui.availableBalance.alpha = 1F
                    ui.networkStatusStateIndicatorView.alpha = 1F
                    ui.balanceGemImageView.alpha = 1F
                }
            )
            start()
        }

        // show interstitial & finish after delay
        ui.scrollView.isScrollable = false
        handler.postDelayed(ONBOARDING_INTERSTITIAL_TIME) {
            ui.scrollView.smoothScrollTo(0, 0)
            ui.blockerView.gone()
        }

    }

    private fun endOnboarding() {
        isOnboarding = false
        ui.onboardingContentView.gone()
        ui.txListHeaderView.visible()
        viewModel.refreshAllData()
        // request Testnet Tari if no txs
        if (viewModel.txListIsEmpty) {
            handler.postDelayed(Constants.UI.xxLongDurationMs, action = viewModel::requestTestnetTari)
        }
    }

    companion object {
        private const val GRABBER_ALPHA_SCROLL_COEFFICIENT = 1.2F
        private const val GRABBER_WIDTH_SCROLL_COEFFICIENT = 1.1F
        private const val GRABBER_CORNER_RADIUS_SCROLL_COEFFICIENT = 1.4F
        private const val ONBOARDING_INTERSTITIAL_TIME = 4500L
    }
}


