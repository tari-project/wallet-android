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
package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.disabled_cta
import com.tari.android.wallet.R.dimen.create_wallet_button_bottom_margin
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentCreateWalletBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.yat.YatService
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.model.yat.EmojiId
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateEmojiId
import com.tari.android.wallet.util.SharedPrefsWrapper
import javax.inject.Inject

/**
 * onBoarding flow : wallet creation step.
 *
 * @author The Tari Development Team
 */
class CreateWalletFragment : Fragment(), WalletCreationStateVisitor {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var service: YatService

    @Inject
    lateinit var yatAdapter: YatAdapter

    private lateinit var viewModel: WalletCreationViewModel

    lateinit var ui: FragmentCreateWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentCreateWalletBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        viewModel = ViewModelProvider(this, WalletCreationViewModelFactory(service)).get()
        observeYatState()
        if (savedInstanceState == null)
            tracker.screen(path = "/onboarding/create_wallet", title = "Onboarding - Create Wallet")
    }

    private fun observeYatState() {
        viewModel.state.observe(viewLifecycleOwner) { it.dispatch(this) }
        yatAdapter.state.observe(viewLifecycleOwner) { viewModel.handleYatState(it) }
    }

    override fun onInitial() = with(ui) {
        yourEmojiIdTitleTextView.text =
            string(create_wallet_your_emoji_id_text_label).applyFontStyle(
                requireActivity(),
                CustomFont.AVENIR_LT_STD_LIGHT,
                listOf(string(create_wallet_your_emoji_id_text_label_highlighted_part)),
                CustomFont.AVENIR_LT_STD_BLACK
            )
        bottomSpinnerLottieAnimationView.alpha = 0F
        continueCtaView.alpha = 0F
        createEmojiIdContainer.alpha = 0F
        continueCtaView.gone()
        emojiIdSummaryContainerView.invisible()
        createEmojiIdProgressBar.setColor(Color.WHITE)
        continueButtonProgressBar.setColor(Color.WHITE)
        rootView.doOnGlobalLayout {
            whiteBgView.translationY = -whiteBgView.height.toFloat()
            playStartupWhiteBgAnimation()
            createEmojiIdContainer.setBottomMargin(
                createEmojiIdContainer.height * -2
            )
        }
        createEmojiIdContainer.setOnClickListener { onCreateEmojiIdButtonClick() }
        continueCtaView.setOnClickListener { onContinueButtonClick() }
        getDifferentYatCtaTextView.setOnClickListener { onYatRerollButtonClick() }

    }

    private fun playStartupWhiteBgAnimation() = ui.whiteBgView.createObjectAnimator(
        View.TRANSLATION_Y,
        values = floatArrayOf(-ui.whiteBgView.height.toFloat(), 0F),
        duration = CreateEmojiId.whiteBgAnimDurationMs,
        interpolator = EasingInterpolator(Ease.CIRC_IN_OUT),
        onStart = {
            ui.smallGemImageView.visible()
            ui.whiteBgView.visible()
        },
        onEnd = {
            showBottomSpinner()
            showSecondViewByAnim()
        },
    ).start()

    private fun showBottomSpinner() = ui.bottomSpinnerLottieAnimationView.createObjectAnimator(
        property = View.ALPHA,
        values = floatArrayOf(0F, 1F),
        duration = Constants.UI.longDurationMs
    ).start()

    private fun hideBottomSpinner() = ui.bottomSpinnerLottieAnimationView.createObjectAnimator(
        property = View.ALPHA,
        values = floatArrayOf(1F, 0F),
        duration = CreateEmojiId.shortAlphaAnimDuration
    ).start()

    private fun showSecondViewByAnim() {
        ui.justSecDescBackView.visible()
        ui.justSecTitleBackView.visible()
        val offset = -ui.justSecTitleTextView.height.toFloat()

        val titleAnim = ui.justSecTitleTextView.createObjectAnimator(
            property = View.TRANSLATION_Y,
            values = floatArrayOf(0F, offset),
            interpolator = EasingInterpolator(Ease.QUINT_OUT),
            startDelay = CreateEmojiId.titleShortAnimDelayMs,
        )

        val descAnim = ui.justSecDescTextView.createObjectAnimator(
            property = View.TRANSLATION_Y,
            values = floatArrayOf(0F, offset),
            interpolator = EasingInterpolator(Ease.QUINT_OUT)
        )

        animatorSetOf(
            duration = CreateEmojiId.helloTextAnimDurationMs,
            interpolator = EasingInterpolator(Ease.QUART_OUT),
            onStart = {
                ui.justSecDescTextView.visible()
                ui.justSecTitleTextView.visible()
            },
            onEnd = {
                ui.rootView.postDelayed(
                    CreateEmojiId.viewChangeAnimDelayMs,
                    this::awaitForConnectionAndProceed
                )
            },
            children = playTogether(titleAnim, descAnim)
        ).start()
    }

    private fun awaitForConnectionAndProceed() {
        if (EventBus.walletStateSubject.value == WalletState.RUNNING
            && viewModel.state.value is InitialState
        ) {
            startCheckMarkAnimation()
        } else {
            EventBus.subscribeToWalletState(this, ::onWalletStateChanged)
        }
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING && viewModel.state.value is InitialState) {
            ui.rootView.post { startCheckMarkAnimation() }
        }
    }

    private fun startCheckMarkAnimation() {
        ui.justSecDescBackView.gone()
        ui.justSecTitleBackView.gone()

        ui.checkmarkLottieAnimationView.addAnimatorListener(
            onEnd = { startCreateEmojiIdAnimation() }
        )

        hideBottomSpinner()

        animateValues(
            values = floatArrayOf(1F, 0F),
            duration = CreateEmojiId.shortAlphaAnimDuration,
            onUpdate = {
                val alpha = it.animatedValue as Float
                ui.justSecDescTextView.alpha = alpha
                ui.justSecTitleTextView.alpha = alpha
            },
            onEnd = {
                ui.checkmarkLottieAnimationView.visible()
                ui.checkmarkLottieAnimationView.playAnimation()
            }
        ).start()

    }

    private fun startCreateEmojiIdAnimation() {
        // animation is looping, so we have to skip the fade-in and scale-up anims
        // that happen at the beginning of the animation
        ui.nerdFaceEmojiLottieAnimationView.setMinFrame(50)
        ui.nerdFaceEmojiLottieAnimationView.translationY =
            -(ui.nerdFaceEmojiLottieAnimationView.height).toFloat()
        ui.nerdFaceEmojiLottieAnimationView.playAnimation()

        val buttonInitialBottomMargin = ui.createEmojiIdContainer.getBottomMargin()
        val buttonBottomMarginDelta =
            dimenPx(create_wallet_button_bottom_margin) - buttonInitialBottomMargin

        val fadeInAnim = animateValues(values = floatArrayOf(0F, 1F), onUpdate = {
            val alpha = it.animatedValue as Float
            ui.walletAddressDescTextView.alpha = alpha
            ui.createEmojiIdContainer.alpha = alpha
            ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
            ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
        })

        val awesomeAnim = ui.createYourEmojiIdLine1TextView.createObjectAnimator(
            property = View.TRANSLATION_Y,
            values = floatArrayOf(0F, -ui.createYourEmojiIdLine1TextView.height.toFloat()),
            duration = CreateEmojiId.awesomeTextAnimDurationMs,
        )

        val createNowAnim = ui.createYourEmojiIdLine2TextView.createObjectAnimator(
            property = View.TRANSLATION_Y,
            values = floatArrayOf(0F, -ui.createYourEmojiIdLine2TextView.height.toFloat()),
            duration = CreateEmojiId.awesomeTextAnimDurationMs,
        )

        val buttonTranslationAnim = animateValues(
            values = floatArrayOf(0F, 1F),
            duration = CreateEmojiId.awesomeTextAnimDurationMs,
            startDelay = CreateEmojiId.createEmojiButtonAnimDelayMs,
            onUpdate = {
                val value = it.animatedValue as Float
                ui.createEmojiIdContainer.setBottomMargin(
                    (buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt()
                )
            }
        )

        animatorSetOf(
            startDelay = CreateEmojiId.viewOverlapDelayMs,
            duration = CreateEmojiId.createEmojiViewAnimDurationMs,
            interpolator = EasingInterpolator(Ease.QUINT_OUT),
            onStart = {
                ui.createEmojiIdContainer.visible()
                ui.createYourEmojiIdLine2BlockerView.visible()
                ui.createYourEmojiIdLine1BlockerView.visible()
                ui.createYourEmojiIdLine1TextView.visible()
                ui.createYourEmojiIdLine2TextView.visible()
                ui.createYourEmojiIdLine1TextView.alpha = 1F
                ui.createYourEmojiIdLine2TextView.alpha = 1F
                ui.createYourEmojiIdLine1TextView.translationY = 0F
                ui.createYourEmojiIdLine2TextView.translationY = 0F
                ui.justSecDescBackView.gone()
                ui.justSecTitleBackView.gone()
            },
            children = playTogether(fadeInAnim, createNowAnim, awesomeAnim, buttonTranslationAnim)
        ).start()
    }

    private fun onCreateEmojiIdButtonClick() {
        ui.createEmojiIdContainer.animateClick {
            showEmojiWheelAnimation {
                viewModel.onStart { yatAdapter.start(requireActivity()) }
            }
        }
    }

    private fun showEmojiWheelAnimation(onEnd: () -> Unit) {
        animateValues(
            values = floatArrayOf(1F, 0F),
            startDelay = CreateEmojiId.walletCreationFadeOutAnimDelayMs,
            duration = CreateEmojiId.walletCreationFadeOutAnimDurationMs,
            onUpdate = {
                val alpha = it.animatedValue as Float
                ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
                ui.createYourEmojiIdLine1TextView.alpha = alpha
                ui.createYourEmojiIdLine2TextView.alpha = alpha
                ui.walletAddressDescTextView.alpha = alpha
                ui.createEmojiIdContainer.alpha = alpha
            },
            onEnd = {
                ui.createEmojiIdContainer.gone()
                ui.createYourEmojiIdLine1BlockerView.gone()
                ui.justSecTitleBackView.gone()
                ui.justSecDescBackView.gone()
                ui.createYourEmojiIdLine2BlockerView.gone()
            },
        ).start()
        ui.emojiWheelLottieAnimationView.playAnimation()
        ui.rootView.postDelayed(ui.emojiWheelLottieAnimationView.duration) {
            onEnd()
        }
    }

    private fun startYourEmojiIdViewAnimation() {
        tracker.screen(
            path = "/onboarding/create_emoji_id",
            title = "Onboarding - Create Emoji Id"
        )

        ui.createEmojiIdContainer.gone()
        ui.createYourEmojiIdLine1BlockerView.gone()
        ui.justSecTitleBackView.gone()
        ui.justSecDescBackView.gone()
        ui.createYourEmojiIdLine2BlockerView.gone()

        ui.nerdFaceEmojiLottieAnimationView.alpha = 0f
        ui.createYourEmojiIdLine1TextView.alpha = 0f
        ui.createYourEmojiIdLine2TextView.alpha = 0f
        ui.walletAddressDescTextView.alpha = 0f
        ui.createEmojiIdContainer.alpha = 0f

        val shouldAnimateContinueButton = ui.continueCtaView.visibility == View.GONE

        val fadeIn = animateValues(
            values = floatArrayOf(0F, 1F),
            interpolator = EasingInterpolator(Ease.QUINT_IN),
            duration = CreateEmojiId.continueButtonAnimDurationMs,
            onUpdate = {
                val alpha = it.animatedValue as Float
                ui.emojiIdSummaryContainerView.alpha = alpha
                ui.emojiIdDescTextView.alpha = alpha
                ui.getDifferentYatCtaTextView.alpha = alpha
                if (shouldAnimateContinueButton) ui.continueCtaView.alpha = alpha
            },
        )
        val lift = animateValues(
            values = floatArrayOf(ui.yourEmojiIdTitleTextView.height.toFloat(), 0F),
            startDelay = CreateEmojiId.yourEmojiIdTextAnimDelayMs,
            duration = CreateEmojiId.yourEmojiIdTextAnimDurationMs,
            onStart = { ui.yourEmojiIdTitleContainerView.visible() },
            onUpdate = { ui.yourEmojiIdTitleTextView.translationY = it.animatedValue as Float }
        )
        animatorSetOf(
            onStart = {
                ui.emojiIdSummaryContainerView.visible()
                ui.getDifferentYatCtaTextView.visible()
                ui.continueCtaView.visible()
                ui.continueButtonLabel.visible()
                ui.continueButtonProgressBar.gone()
                ui.getDifferentYatCtaTextView.isClickable = false
                ui.continueCtaView.isClickable = false
            },
            onEnd = {
                ui.getDifferentYatCtaTextView.isClickable = true
                ui.continueCtaView.isClickable = true
            },
            children = playTogether(fadeIn, lift),
        ).start()
    }

    private fun onYatRerollButtonClick() =
        viewModel.onStartAgain { yatAdapter.start(requireActivity()) }

    private fun onContinueButtonClick() {
        ui.continueCtaView.animateClick()
        viewModel.onYatAcquireInitiated(yatAdapter.state.value?.yat.orEmpty())
    }

    fun fadeOutAllViewAnimation() = animateValues(
        values = floatArrayOf(1F, 0F),
        duration = Constants.UI.mediumDurationMs,
        onUpdate = {
            val alpha = it.animatedValue as Float
            ui.continueCtaView.alpha = alpha
            ui.emojiIdDescTextView.alpha = alpha
            ui.getDifferentYatCtaTextView.alpha = alpha
            ui.emojiIdSummaryContainerView.alpha = alpha
            ui.yourEmojiIdTitleContainerView.alpha = alpha
        }
    ).start()

    override fun onSearchingForInitialYat() = Unit

    override fun onInitialYatFound(yat: EmojiId) {
        ui.emojiIdTextView.text = yat.raw
        startYourEmojiIdViewAnimation()
    }

    override fun onInitialYatSearchError(exception: Exception) {
        ui.createEmojiIdContainer.visible()
        ui.createYourEmojiIdLine1BlockerView.visible()
        ui.justSecTitleBackView.visible()
        ui.justSecDescBackView.visible()
        ui.createYourEmojiIdLine2BlockerView.visible()

        ui.nerdFaceEmojiLottieAnimationView.alpha = 1f
        ui.createYourEmojiIdLine1TextView.alpha = 1f
        ui.createYourEmojiIdLine2TextView.alpha = 1f
        ui.walletAddressDescTextView.alpha = 1f
        ui.createEmojiIdContainer.alpha = 1f

        ui.createEmojiIdContainer.setBottomMargin(dimenPx(create_wallet_button_bottom_margin))

        hideBottomSpinner()

        Logger.e(exception, "Exception during yat generation")
        showWalletCreationError {
            viewModel.onStart { yatAdapter.start(requireActivity()) }
            ui.continueCtaView.gone()
        }
    }

    private fun showWalletCreationError(onClose: () -> Unit) {
        ErrorDialog(
            requireContext(),
            string(create_wallet_error_occurred_title),
            string(create_wallet_error_occurred_description),
            onClose = onClose,
            closeButtonTextResourceId = common_retry
        ).show()
    }

    override fun onYatAcquiring() {
        ui.getDifferentYatCtaTextView.isClickable = false
        ui.getDifferentYatCtaTextView.setTextColor(color(disabled_cta))
        ui.continueCtaView.isClickable = false
        ui.continueButtonProgressBar.visible()
        ui.continueButtonLabel.gone()
    }

    override fun onYatAcquired() {
        sharedPrefsWrapper.onboardingCompleted = true
        sharedPrefsWrapper.onboardingAuthSetupStarted = true
        (activity as? Listener)?.onWalletCreated()
    }

    interface Listener {
        fun onWalletCreated()
    }

    class WalletCreationViewModelFactory(private val service: YatService) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(modelClass === WalletCreationViewModel::class.java)
            return WalletCreationViewModel(service) as T
        }
    }
}
