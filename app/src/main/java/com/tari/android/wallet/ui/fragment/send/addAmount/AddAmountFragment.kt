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
package com.tari.android.wallet.ui.fragment.send.addAmount

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.amountInputBinding.fragment.send.addAmount.keyboard.KeyboardController
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.FragmentAddAmountBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.dialog.tooltipDialog.TooltipDialog
import com.tari.android.wallet.ui.dialog.tooltipDialog.TooltipDialogArgs
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

class AddAmountFragment : CommonFragment<FragmentAddAmountBinding, AddAmountViewModel>(), ServiceConnection {

    @Inject
    lateinit var tracker: Tracker

    private lateinit var addAmountListenerWR: WeakReference<AddAmountListener>

    /**
     * Recipient is either an emoji id or a user from contacts or recent txs.
     */
    private var recipientUser: User? = null

    /**
     *     Control full emoji popups
     */
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    /**
     * Formats the summarized emoji id.
     */
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    private var keyboardController: KeyboardController = KeyboardController()

    private lateinit var walletService: TariWalletService

    private var isFirstLaunch: Boolean = false

    private var estimatedFee: MicroTari? = null
    private lateinit var balanceInfo: BalanceInfo
    private lateinit var availableBalance: MicroTari

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddAmountBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)

        val viewModel: AddAmountViewModel by viewModels()
        bindViewModel(viewModel)
        subscribeVM()

        bindToWalletService()
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_amount", title = "Send Tari - Add Amount")
        }
        isFirstLaunch = savedInstanceState == null
    }

    private fun subscribeVM() = with(viewModel) {
        observe(isOneSidePaymentEnabled) { ui.oneSidePaymentSwitchView.isChecked = it }
    }

    private fun bindToWalletService() {
        val bindIntent = Intent(requireActivity(), WalletService::class.java)
        requireActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.i("AddAmountFragment onServiceConnected")
        walletService = TariWalletService.Stub.asInterface(service)
        // Only binding UI if we have not passed `onDestroyView` line, which is a possibility
        setupUI()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.i("AddAmountFragment onServiceDisconnected")
        // No-op for now
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unbindService(this)
    }

    private fun setupUI() {
        val amount = arguments?.getDouble(DeepLink.PARAMETER_AMOUNT, Double.MIN_VALUE)
        keyboardController.setup(requireContext(), AmountCheckRunnable(), ui.numpad, ui.amount, amount)
        recipientUser = arguments?.getParcelable("recipientUser")
        // hide tx fee
        ui.txFeeContainerView.invisible()
        // hide/disable continue button
        ui.continueButton.invisible()
        ui.disabledContinueButton.visible()
        // add first digit to the element list
        val fullEmojiIdListener = object : FullEmojiIdViewController.Listener {
            override fun animationHide(value: Float) {
                ui.backButton.alpha = 1 - value
            }

            override fun animationShow(value: Float) {
                ui.backButton.alpha = 1 - value
            }
        }
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext(),
            fullEmojiIdListener
        )
        fullEmojiIdViewController.fullEmojiId = recipientUser?.publicKey?.emojiId.orEmpty()
        fullEmojiIdViewController.emojiIdHex = recipientUser?.publicKey?.hexString.orEmpty()
        displayAliasOrEmojiId()
        setActionBindings()
    }

    private fun setActionBindings() {
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
        ui.txFeeDescTextView.setOnClickListener { showTxFeeToolTip() }
        ui.oneSidePaymentHelp.setOnClickListener { showOneSidePaymentTooltip() }
        ui.continueButton.setOnClickListener { continueButtonClicked() }
        ui.oneSidePaymentSwitchView.setOnClickListener { viewModel.toggleOneSidePayment() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        addAmountListenerWR = WeakReference(context as AddAmountListener)
    }

    private fun displayAliasOrEmojiId() {
        if (recipientUser is Contact) {
            ui.emojiIdSummaryContainerView.gone()
            ui.titleTextView.visible()
            ui.titleTextView.text = (recipientUser as Contact).alias
        } else {
            recipientUser?.publicKey?.emojiId?.let {
                displayEmojiId(it)
            }
        }
    }

    private fun displayEmojiId(emojiId: String) {
        ui.emojiIdSummaryContainerView.visible()
        emojiIdSummaryController.display(emojiId)
        ui.titleTextView.gone()
    }

    private fun onBackButtonClicked(view: View) {
        view.temporarilyDisableClick()
        val mActivity = activity ?: return
        mActivity.onBackPressed()
    }

    /**
     * Display full emoji id and dim out all other views.
     */
    private fun emojiIdClicked() {
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun showTxFeeToolTip() {
        TooltipDialog(requireContext(), TooltipDialogArgs(string(tx_detail_fee_tooltip_transaction_fee), string(tx_detail_fee_tooltip_desc))).show()
    }

    private fun showOneSidePaymentTooltip() {
        val args = TooltipDialogArgs(string(add_amount_one_side_payment_switcher), string(add_amount_one_side_payment_question_mark))
        TooltipDialog(requireContext(), args).show()
    }

    private fun continueButtonClicked() {
        ui.continueButton.isClickable = false
        lifecycleScope.launch(Dispatchers.IO) { checkAmountAndFee() }
    }

    private fun checkAmountAndFee() {
        val error = WalletError()
        val balanceInfo = walletService.getBalanceInfo(error)
        val fee = estimatedFee
        val amount = keyboardController.currentAmount
        if (error.code == WalletErrorCode.NO_ERROR && fee != null) {
            if (amount > balanceInfo.availableBalance) {
                lifecycleScope.launch(Dispatchers.Main) {
                    actualBalanceExceeded()
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (fee > amount) {
                        ErrorDialog(
                            requireActivity(),
                            title = string(error_fee_more_than_amount_title),
                            description = string(error_fee_more_than_amount_description),
                            canceledOnTouchOutside = true
                        ).show()
                        ui.continueButton.isClickable = true
                    } else {
                        continueToNote()
                        ui.continueButton.isClickable = true
                    }
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                ui.continueButton.isClickable = true
            }
        }
    }

    private fun updateBalanceInfo() {
        val error = WalletError()

        balanceInfo = walletService.getBalanceInfo(error)
        if (error.code != WalletErrorCode.NO_ERROR) {
            TODO("Unhandled wallet error: ${error.code}")
        }

        availableBalance = balanceInfo.availableBalance + balanceInfo.pendingIncomingBalance

        ui.availableBalanceTextView.text =
            WalletUtil.balanceFormatter.format(availableBalance.tariValue)
    }

    private fun actualBalanceExceeded() {
        addAmountListenerWR.get()?.onAmountExceedsActualAvailableBalance(this)
        ui.continueButton.isClickable = true
    }

    private fun continueToNote() {
        addAmountListenerWR.get()?.continueToAddNote(recipientUser!!, keyboardController.currentAmount, ui.oneSidePaymentSwitchView.isChecked)
    }

    /**
     * Checks/validates the amount entered.
     */
    private inner class AmountCheckRunnable : Runnable {

        override fun run() {
            val error = WalletError()

            // update fee
            val fee = walletService.estimateTxFee(keyboardController.currentAmount, error)
            estimatedFee = fee

            if (error.code != WalletErrorCode.NO_ERROR
                && error.code != WalletErrorCode.NOT_ENOUGH_FUNDS
                && error.code != WalletErrorCode.FUNDS_PENDING
            ) {
                TODO("Unhandled wallet error: ${error.code}")
            }

            updateBalanceInfo()

            if (error.code == WalletErrorCode.FUNDS_PENDING
                || error.code == WalletErrorCode.NOT_ENOUGH_FUNDS
                || (keyboardController.currentAmount + fee) > availableBalance
            ) {
                showErrorState(error)
            } else {
                showSuccessState(fee)
            }
        }

        @SuppressLint("SetTextI18n")
        private fun showSuccessState(fee: MicroTari) = with(ui) {
            notEnoughBalanceDescriptionTextView.text = string(add_amount_wallet_balance)
            availableBalanceContainerView.visible()

            val showsTxFee: Boolean
            txFeeTextView.text = "+${WalletUtil.amountFormatter.format(fee.tariValue)}"
            // show/hide continue button
            if (keyboardController.currentAmount.value.toInt() == 0) {
                hideContinueButton()
                showsTxFee = false
            } else {
                showContinueButtonAnimated()
                showsTxFee = true
            }

            showBalance()

            showOrHideFeeViewAnimated(showsTxFee)
        }

        private fun showOrHideFeeViewAnimated(showsTxFee: Boolean) = with(ui) {
            if (showsTxFee && txFeeContainerView.visibility == View.VISIBLE ||
                !showsTxFee && txFeeContainerView.visibility == View.INVISIBLE
            ) {
                return@with
            }

            if (showsTxFee) {
                txFeeContainerView.alpha = 0f
                txFeeContainerView.visible()
            }

            ValueAnimator.ofFloat(0f, 1f).apply {
                addUpdateListener { valueAnimator: ValueAnimator ->
                    val value = valueAnimator.animatedValue as Float

                    if (showsTxFee) {
                        txFeeContainerView.translationY = (1f - value) * 100
                        txFeeContainerView.alpha = value
                    } else {
                        txFeeContainerView.translationY = value * 100
                        txFeeContainerView.alpha = (1f - value)

                        if (value == 1f) {
                            txFeeContainerView.invisible()
                        }
                    }
                }
                duration = Constants.UI.shortDurationMs
                interpolator = EasingInterpolator(Ease.CIRC_OUT)
                start()
            }
        }

        private fun showErrorState(
            error: WalletError
        ) = with(ui) {
            if (error.code == WalletErrorCode.FUNDS_PENDING) {
                availableBalanceContainerView.gone()
                notEnoughBalanceDescriptionTextView.text =
                    string(add_amount_funds_pending)
            } else {
                availableBalanceContainerView.visible()
                notEnoughBalanceDescriptionTextView.text =
                    string(add_amount_not_enough_available_balance)
            }

            hideContinueButton()

            showAvailableBalanceError()

            keyboardController.nudgeAmountView()

            showOrHideFeeViewAnimated(true)
        }

        private fun showAvailableBalanceError() = with(ui) {
            notEnoughBalanceView.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.validation_error_box_border_bg
            )
            gemNotEnoughBalance.imageTintList = null
            availableBalanceTextView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.common_error
                )
            )
        }

        private fun showBalance() = with(ui) {
            notEnoughBalanceView.background = null
            gemNotEnoughBalance.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), black))
            availableBalanceTextView.setTextColor(ContextCompat.getColor(requireContext(), black))
        }

        private fun showContinueButtonAnimated() = with(ui) {
            if (continueButton.visibility == View.VISIBLE) {
                return@with
            }
            continueButton.alpha = 0f
            continueButton.visible()
            ObjectAnimator.ofFloat(continueButton, "alpha", 0f, 1f).apply {
                duration = Constants.UI.shortDurationMs
                start()
            }
        }

        private fun hideContinueButton() = with(ui) {
            continueButton.invisible()
        }
    }
}