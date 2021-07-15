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
package com.tari.android.wallet.ui.activity.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ActivityDeleteWalletBinding
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.extension.ThrottleClick
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteWalletActivity : AppCompatActivity() {

    private var ui: ActivityDeleteWalletBinding? = null
    private lateinit var serviceConnection: TariWalletServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        appComponent.inject(this)
        serviceConnection = ViewModelProvider(this)[TariWalletServiceConnection::class.java]
        setupUI()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private fun setupUI() {
        ui = ActivityDeleteWalletBinding.inflate(layoutInflater).apply { setContentView(root) }
        ui?.deleteWalletProgress?.setColor(color(R.color.common_error))
        ui?.backCtaView?.setOnClickListener(ThrottleClick { onBackPressed() })
        ui?.deleteWalletCtaView?.setOnClickListener(ThrottleClick { confirmDeleteWallet() })
    }

    private fun confirmDeleteWallet() {
        BottomSlideDialog(
            this,
            R.layout.dialog_confirm_delete_wallet,
            canceledOnTouchOutside = true
        ).apply {
            findViewById<View>(R.id.delete_wallet_confirm_button)
                .setOnClickListener(ThrottleClick {
                    deleteWallet()
                    dismiss()
                })
            findViewById<View>(R.id.delete_wallet_cancel_button)
                .setOnClickListener(ThrottleClick {
                    dismiss()
                })
        }.show()
    }

    private fun deleteWallet() {
        // disable CTAs
        ui?.backCtaView?.isEnabled = false
        ui?.deleteWalletCtaView?.isEnabled = false
        ui?.deleteWalletProgress?.visible()
        // delete wallet
        lifecycleScope.launch(Dispatchers.IO) {
            WalletService.stopAndDelete(applicationContext)
            withContext(Dispatchers.Main) {
                goToSplashScreen()
            }
        }
    }

    private fun goToSplashScreen() {
        val intent = Intent(this, OnboardingFlowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finishAffinity()
    }

}