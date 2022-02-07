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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.amountInputBinding.fragment.send.addAmount.keyboard.KeyboardController
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.FragmentAddAmountBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Amount entry fragment.
 *
 * @author The Tari Development Team
 */
class AddAmountFragment : Fragment(), ServiceConnection {

    @Inject
    lateinit var tracker: Tracker

    private lateinit var listenerWR: WeakReference<Listener>

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
    private lateinit var ui: FragmentAddAmountBinding

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
        bindToWalletService()
        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_amount", title = "Send Tari - Add Amount")
        }
        isFirstLaunch = savedInstanceState == null
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
        ui.txFeeDescTextView.setOnClickListener { onFeeViewClick() }
        ui.continueButton.setOnClickListener { continueButtonClicked() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
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

    private fun onFeeViewClick() {
        showTxFeeToolTip()
    }

    private fun showTxFeeToolTip() {
        BottomSlideDialog(
            context = activity ?: return,
            layoutId = R.layout.tx_fee_tooltip_dialog,
            dismissViewId = R.id.tx_fee_tooltip_dialog_txt_close
        ).show()
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
        if (error == WalletError.NoError && fee != null) {
            if (amount > balanceInfo.availableBalance) {
                lifecycleScope.launch(Dispatchers.Main) {
                    actualBalanceExceeded()
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (fee > amount) {
                        val args = ErrorDialogArgs(string(R.string.error_fee_more_than_amount_title), string(R.string.error_fee_more_than_amount_description))
                        ErrorDialog(requireContext(), args).show()
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
        balanceInfo = walletService.getWithError { error, wallet -> wallet.getBalanceInfo(error) }
        availableBalance = balanceInfo.availableBalance + balanceInfo.pendingIncomingBalance
        ui.availableBalanceTextView.text = WalletUtil.balanceFormatter.format(availableBalance.tariValue)
    }

    private fun actualBalanceExceeded() {
        listenerWR.get()?.onAmountExceedsActualAvailableBalance(this)
        ui.continueButton.isClickable = true
    }

    private fun continueToNote() {
        listenerWR.get()?.continueToAddNote(this, recipientUser!!, keyboardController.currentAmount)
    }

    /**
     * Checks/validates the amount entered.
     */
    private inner class AmountCheckRunnable : Runnable {

        override fun run() {
            val error = WalletError()

            estimatedFee = walletService.estimateTxFee(keyboardController.currentAmount, error)

            //todo
//            && error.code != WalletError.NOT_ENOUGH_FUNDS
//            && error.code != WalletError.FUNDS_PENDING
            throwIf(error)

            updateBalanceInfo()

//            error.code == WalletError.FUNDS_PENDING
//                || error.code == WalletError.NOT_ENOUGH_FUNDS
            if ((keyboardController.currentAmount + estimatedFee!!) > availableBalance) {
                showErrorState(error)
            } else {
                showSuccessState(estimatedFee!!)
            }
        }

        @SuppressLint("SetTextI18n")
        private fun showSuccessState(fee: MicroTari) = with(ui) {
            notEnoughBalanceDescriptionTextView.text = string(R.string.add_amount_wallet_balance)
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
            //todo
            if (error.code == 115) {
                availableBalanceContainerView.gone()
                notEnoughBalanceDescriptionTextView.text =
                    string(R.string.add_amount_funds_pending)
            } else {
                availableBalanceContainerView.visible()
                notEnoughBalanceDescriptionTextView.text =
                    string(R.string.add_amount_not_enough_available_balance)
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

    // region listener interface

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment)

        /**
         * Recipient is user.
         */
        fun continueToAddNote(
            sourceFragment: AddAmountFragment,
            recipientUser: User,
            amount: MicroTari
        )

    }

    // endregion
}
