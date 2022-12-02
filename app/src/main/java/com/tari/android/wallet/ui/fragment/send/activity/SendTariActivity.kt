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
package com.tari.android.wallet.ui.fragment.send.activity

import android.os.Bundle
import androidx.activity.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ActivitySendTariBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.send.addAmount.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.addAmount.AddAmountListener
import com.tari.android.wallet.ui.fragment.send.addNote.AddNodeListener
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientListener
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxListener
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.send.makeTransaction.MakeTransactionFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.yat.YatAdapter
import com.tari.android.wallet.yat.YatUser
import java.lang.ref.WeakReference
import javax.inject.Inject


/**
 * The host activity for all send-related fragments.
 *
 * @author The Tari Development Team
 */
class SendTariActivity : CommonActivity<ActivitySendTariBinding, SendTariViewModel>(),
    AddRecipientListener,
    AddAmountListener,
    AddNodeListener,
    FinalizeSendTxListener {

    @Inject
    lateinit var yatAdapter: YatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = WeakReference(this)
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivitySendTariBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: SendTariViewModel by viewModels()
        bindViewModel(viewModel)

        setContainerId(R.id.send_tari_fragment_container_view)

        if (savedInstanceState == null) {
            loadRootFragment()
        }
    }

    /**
     * Loads initial fragment.
     */
    private fun loadRootFragment() {
        val recipientUser = intent.parcelable<User>("recipientUser")
        if (recipientUser != null) {
            val bundle = Bundle().apply {
                putParcelable("recipientUser", recipientUser)
                intent.getDoubleExtra(PARAMETER_AMOUNT, Double.MIN_VALUE).takeIf { it > 0 }?.let { putDouble(PARAMETER_AMOUNT, it) }
            }
            addFragment(AddAmountFragment(), bundle, true)
        } else {
            addFragment(MakeTransactionFragment(), null, true)
        }
        ui.rootView.postDelayed({ ui.rootView.setBackgroundColor(color(R.color.black)) }, 1000)
    }

    override fun continueToAmount(user: User, amount: MicroTari?) {
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this)
            return
        }
        hideKeyboard()
        val bundle = Bundle().apply {
            putParcelable(PARAMETER_USER, user)
            putParcelable(PARAMETER_AMOUNT, amount)
        }
        ui.rootView.postDelayed({ addFragment(AddAmountFragment(), bundle) }, Constants.UI.keyboardHideWaitMs)
    }

    override fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment) {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(string(R.string.error_balance_exceeded_title)),
                BodyModule(string(R.string.error_balance_exceeded_description)),
                ButtonModule(string(R.string.common_close), ButtonStyle.Close),
            )
        )
        ModularDialog(this, args).show()
    }

    override fun continueToAddNote(transactionData: TransactionData) {
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this)
            return
        }
        val bundle = Bundle().apply {
            putParcelable("transactionData", transactionData)
            intent.getStringExtra(PARAMETER_NOTE)?.let { putString(PARAMETER_NOTE, it) }
        }
        addFragment(AddNoteFragment(), bundle)
    }

    override fun continueToFinalizing(transactionData: TransactionData) {
        continueToFinalizeSendTx(transactionData)
    }

    override fun continueToFinalizeSendTx(transactionData: TransactionData) {
        if (transactionData.recipientUser is YatUser) {
            yatAdapter.showOutcomingFinalizeActivity(this, transactionData)
        } else {
            addFragment(FinalizeSendTxFragment.create(transactionData))
            ui.rootView.post { ui.rootView.setBackgroundColor(color(R.color.white)) }
        }
    }

    override fun onSendTxFailure(transactionData: TransactionData, txFailureReason: TxFailureReason) {
        EventBus.post(Event.Transaction.TxSendFailed(txFailureReason))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onSendTxSuccessful(txId: TxId, transactionData: TransactionData) {
        EventBus.post(Event.Transaction.TxSendSuccessful(txId))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onDestroy() {
        hideKeyboard()
        super.onDestroy()
    }

    companion object {
        const val PARAMETER_NOTE = "note"
        const val PARAMETER_AMOUNT = "amount"
        const val PARAMETER_USER = "recipientUser"

        var instance: WeakReference<SendTariActivity> = WeakReference(null)
            private set
    }
}