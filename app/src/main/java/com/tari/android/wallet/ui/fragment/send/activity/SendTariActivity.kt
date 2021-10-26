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
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.databinding.ActivitySendTariBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.User
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.addEnterLeftAnimation
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.showInternetConnectionErrorDialog
import com.tari.android.wallet.ui.fragment.send.addAmount.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientListener
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxFragment
import com.tari.android.wallet.util.Constants


/**
 * The host activity for all send-related fragments.
 *
 * @author The Tari Development Team
 */
internal class SendTariActivity : CommonActivity<ActivitySendTariBinding, SendTariViewModel>(),
    AddRecipientListener,
    AddAmountFragment.Listener,
    AddNoteFragment.Listener,
    FinalizeSendTxFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivitySendTariBinding.inflate(layoutInflater).apply { setContentView(root) }
        if (savedInstanceState == null) {
            loadFragment()
        }
    }

    /**
     * Loads initial fragment.
     */
    private fun loadFragment() {
        val recipientUser = intent.getParcelableExtra<User>("recipientUser")
        if (recipientUser != null) {
            val bundle = Bundle().apply {
                putParcelable("recipientUser", recipientUser)
                intent.getDoubleExtra(DeepLink.PARAMETER_AMOUNT, Double.MIN_VALUE)
                    .takeIf { it > 0 }
                    ?.let { putDouble(DeepLink.PARAMETER_AMOUNT, it) }
            }
            addFragment(AddAmountFragment(), bundle, true)
        } else {
            addFragment(AddRecipientFragment(), null, true)
        }
        ui.rootView.postDelayed({ ui.rootView.setBackgroundColor(color(R.color.black)) }, 1000)
    }

    override fun continueToAmount(sourceFragment: AddRecipientFragment, user: User) {
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this)
            return
        }
        hideKeyboard()
        val bundle = Bundle().apply { putParcelable("recipientUser", user) }
        ui.rootView.postDelayed({ addFragment(AddAmountFragment(), bundle) }, Constants.UI.keyboardHideWaitMs)
    }

    override fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment) {
        BottomSlideDialog(
            context = this,
            layoutId = R.layout.add_amount_dialog_actual_balance_exceeded,
            dismissViewId = R.id.add_amount_dialog_btn_close
        ).show()
    }

    override fun continueToAddNote(sourceFragment: AddAmountFragment, recipientUser: User, amount: MicroTari) {
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            showInternetConnectionErrorDialog(this)
            return
        }
        val bundle = Bundle().apply {
            putParcelable("recipientUser", recipientUser)
            putParcelable("amount", amount)
            intent.getStringExtra(DeepLink.PARAMETER_NOTE)?.let { putString(DeepLink.PARAMETER_NOTE, it) }
        }
        addFragment(AddNoteFragment(), bundle)
    }

    override fun continueToFinalizeSendTx(
        sourceFragment: AddNoteFragment,
        recipientUser: User,
        amount: MicroTari,
        note: String
    ) {
        val bundle = Bundle().apply {
            putParcelable("recipientUser", recipientUser)
            putParcelable("amount", amount)
            putString("note", note)
        }
        addFragment(FinalizeSendTxFragment(), bundle)

        ui.rootView.post { ui.rootView.setBackgroundColor(color(white)) }
    }

    override fun onSendTxFailure(
        sourceFragment: FinalizeSendTxFragment,
        recipientUser: User,
        amount: MicroTari,
        note: String,
        failureReason: FinalizeSendTxFragment.FailureReason
    ) {
        EventBus.post(Event.Transaction.TxSendFailed(failureReason))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onSendTxSuccessful(
        sourceFragment: FinalizeSendTxFragment,
        txId: TxId,
        recipientUser: User,
        amount: MicroTari,
        note: String
    ) {
        EventBus.post(Event.Transaction.TxSendSuccessful(txId))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun addFragment(fragment: Fragment, bundle: Bundle? = null, isRoot: Boolean = false) {
        fragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
            .addEnterLeftAnimation()
            .add(R.id.send_tari_fragment_container_view, fragment, fragment::class.java.simpleName)
        if (!isRoot) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    override fun onDestroy() {
        hideKeyboard()
        super.onDestroy()
    }

    override fun onSendTxStarted(sourceFragment: FinalizeSendTxFragment) = Unit
}