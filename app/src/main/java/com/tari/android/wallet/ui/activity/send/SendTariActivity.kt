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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.fragment.app.FragmentManager
import butterknife.BindView
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.model.User
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.send.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.AmountFragment
import com.tari.android.wallet.ui.util.UiUtil

/**
 * The host activity for all send-related fragments.
 *
 * @author The Tari Development Team
 */
class SendTariActivity : BaseActivity(),
    ServiceConnection, AddRecipientFragment.Listener {

    @BindView(R.id.send_tari_vw_fragment_container)
    lateinit var fragmentContainerView: View

    override val contentViewId = R.layout.activity_send_tari

    private lateinit var mFragmentManager: FragmentManager
    private lateinit var addRecipientFragment: AddRecipientFragment

    private var walletService: TariWalletService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mFragmentManager = supportFragmentManager
    }

    override fun onStart() {
        super.onStart()
        // start service if not started yet
        if (walletService == null) {
            // bind to service
            val bindIntent = Intent(this@SendTariActivity, WalletService::class.java)
            bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Loads initial fragment.
     */
    private fun loadAddRecipientFragment() {
        addRecipientFragment = AddRecipientFragment.newInstance(walletService!!)
        val fragmentTx = mFragmentManager.beginTransaction()
        fragmentTx.replace(R.id.send_tari_vw_fragment_container, addRecipientFragment)
        fragmentTx.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
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
        loadAddRecipientFragment()
    }

    /**
     * Wallet service disconnected.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        Logger.d("Disconnected from the wallet service.")
        walletService = null
    }

    override fun continueToAmount(
        addRecipientFragment: AddRecipientFragment,
        emojiId: String
    ) {
        val bundle = Bundle()
        bundle.putString("emojiId", emojiId)
        goToAmountFragment(addRecipientFragment, bundle)

    }

    override fun continueToAmount(
        addRecipientFragment: AddRecipientFragment,
        user: User
    ) {
        val bundle = Bundle()
        bundle.putParcelable("user", user)
        goToAmountFragment(addRecipientFragment, bundle)
    }

    private fun goToAmountFragment(source: BaseFragment, bundle: Bundle) {
        val amountFragment = AmountFragment.newInstance()
        amountFragment.arguments = bundle
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .hide(source)
            .add(
                R.id.send_tari_vw_fragment_container,
                amountFragment,
                AmountFragment::class.java.simpleName
            )
            .addToBackStack(AmountFragment::class.java.simpleName)
            .commit()
    }

}
