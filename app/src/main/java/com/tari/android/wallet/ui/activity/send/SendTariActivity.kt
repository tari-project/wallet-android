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
package com.tari.android.wallet.ui.activity.send

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import butterknife.BindColor
import butterknife.BindView
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.send.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.AddNoteAndSendFragment
import com.tari.android.wallet.ui.fragment.send.SendTxSuccessfulFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import java.lang.ref.WeakReference

/**
 * The host activity for all send-related fragments.
 *
 * @author The Tari Development Team
 */
internal class SendTariActivity : BaseActivity(),
    ServiceConnection,
    AddRecipientFragment.Listener,
    AddAmountFragment.Listener,
    AddNoteAndSendFragment.Listener,
    SendTxSuccessfulFragment.Listener {

    @BindView(R.id.send_tari_vw_root)
    lateinit var rootView: View
    @BindView(R.id.send_tari_vw_fragment_container)
    lateinit var fragmentContainerView: View

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0
    @BindColor(R.color.black)
    @JvmField
    var blackColor = 0

    override val contentViewId = R.layout.activity_send_tari

    private lateinit var mFragmentManager: FragmentManager
    private var currentFragmentWR: WeakReference<Fragment>? = null

    private var walletService: TariWalletService? = null

    private val wr = WeakReference(this)

    private var sendTxIsInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFragmentManager = supportFragmentManager
    }

    override fun onStart() {
        super.onStart()
        // start service if not started yet
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this, WalletService::class.java)
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
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
            }
            val addAmountFragment = AddAmountFragment.newInstance(walletService!!)
            addAmountFragment.arguments = bundle
            val fragmentTx = mFragmentManager.beginTransaction()
            fragmentTx.add(R.id.send_tari_vw_fragment_container, addAmountFragment)
            fragmentTx.commit()
            currentFragmentWR = WeakReference(addAmountFragment)
            rootView.postDelayed({
                wr.get()?.rootView?.setBackgroundColor(blackColor)
            }, 1000)
        } else {
            val addRecipientFragment = AddRecipientFragment.newInstance(walletService!!)
            val fragmentTx = mFragmentManager.beginTransaction()
            fragmentTx.add(R.id.send_tari_vw_fragment_container, addRecipientFragment)
            fragmentTx.commit()
            currentFragmentWR = WeakReference(addRecipientFragment)
            rootView.postDelayed({
                wr.get()?.rootView?.setBackgroundColor(blackColor)
            }, 1000)
        }
    }

    override fun onBackPressed() {
        if (sendTxIsInProgress) {
            return
        }
        if (currentFragmentWR?.get() is AddAmountFragment
            && supportFragmentManager.fragments[0] is AddRecipientFragment
        ) {
            (supportFragmentManager.fragments[0] as AddRecipientFragment).reset()
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
        currentFragmentWR = WeakReference(supportFragmentManager.fragments.last())
    }

    override fun onDestroy() {
        unbindService(this)
        UiUtil.hideKeyboard(this)
        super.onDestroy()
    }

    /**
     * Wallet service connected.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Logger.d("Connected to the wallet service.")
        walletService = TariWalletService.Stub.asInterface(service)
        loadFragment()
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        walletService = null
    }

    // region AddRecipientFragment.Listener implementation - comments in the interface definition

    override fun continueToAmount(
        sourceFragment: AddRecipientFragment,
        user: User
    ) {
        UiUtil.hideKeyboard(this)
        val bundle = Bundle().apply {
            putParcelable("recipientUser", user)
        }
        rootView.postDelayed({
            wr.get()?.goToAddAmountFragment(sourceFragment, bundle)
        }, Constants.UI.keyboardHideWaitMs)
    }

    private fun goToAddAmountFragment(sourceFragment: BaseFragment, bundle: Bundle) {
        val fragment = AddAmountFragment.newInstance(walletService!!)
        fragment.arguments = bundle
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .hide(sourceFragment)
            .add(
                R.id.send_tari_vw_fragment_container,
                fragment,
                AddAmountFragment::class.java.simpleName
            )
            .addToBackStack(AddAmountFragment::class.java.simpleName)
            .commit()
        currentFragmentWR = WeakReference(fragment)
    }

    /**
     * Display "hold your horses" dialog.
     */
    override fun onAmountExceedsActualAvailableBalance(fragment: AddAmountFragment) {
        Dialog(this, R.style.Theme_AppCompat_Dialog).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.add_amount_dialog_actual_balance_exceeded)
            setCancelable(true)
            window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            findViewById<TextView>(R.id.add_amount_dialog_btn_close)
                .setOnClickListener {
                    dismiss()
                }
            window?.setGravity(Gravity.BOTTOM)
            show()
        }
    }

    // endregion

    // region AddAmountFragment.Listener implementation

    override fun continueToNote(
        sourceFragment: AddAmountFragment,
        recipientUser: User,
        amount: MicroTari,
        fee: MicroTari
    ) {
        val bundle = Bundle().apply {
            putParcelable("recipientUser", recipientUser)
            putParcelable("amount", amount)
            putParcelable("fee", fee)
        }
        goToAddNoteFragment(sourceFragment, bundle)
    }

    private fun goToAddNoteFragment(sourceFragment: BaseFragment, bundle: Bundle) {
        val fragment = AddNoteAndSendFragment.newInstance(walletService!!)
        fragment.arguments = bundle
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .hide(sourceFragment)
            .add(
                R.id.send_tari_vw_fragment_container,
                fragment,
                fragment::class.java.simpleName
            )
            .addToBackStack(AddAmountFragment::class.java.simpleName)
            .commit()
        currentFragmentWR = WeakReference(fragment)
    }

    // endregion

    // region AddNoteFragment.Listener implementation

    override fun sendTxStarted(sourceFragment: AddNoteAndSendFragment) {
        sendTxIsInProgress = true
    }

    override fun sendTxFailed(sourceFragment: AddNoteAndSendFragment) {
        sendTxIsInProgress = false
    }

    override fun sendTxSuccessful(
        sourceFragment: AddNoteAndSendFragment,
        recipientUser: User,
        amount: MicroTari,
        fee: MicroTari,
        note: String
    ) {
        val fragment = SendTxSuccessfulFragment().apply {
            arguments = Bundle().apply {
                putParcelable("recipientUser", recipientUser)
                putParcelable("amount", amount)
                putParcelable("fee", fee)
                putString("note", note)
            }
        }
        rootView.post {
            wr.get()?.rootView?.setBackgroundColor(whiteColor)
        }
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .hide(sourceFragment)
            .add(
                R.id.send_tari_vw_fragment_container,
                fragment,
                fragment::class.java.simpleName
            )
            .addToBackStack(AddAmountFragment::class.java.simpleName)
            .commit()
        currentFragmentWR = WeakReference(fragment)
    }

    // endregion

    // region SendTxSuccessfulFragment.Listener implementation

    override fun sendTxCompleted(
        sourceFragment: SendTxSuccessfulFragment,
        recipientUser: User,
        amount: MicroTari,
        fee: MicroTari,
        note: String
    ) {
        sendTxIsInProgress = false
        EventBus.post(Event.Tx.TxSendSuccessful())
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // endregion

}
