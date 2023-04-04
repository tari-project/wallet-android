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
package com.tari.android.wallet.ui.fragment.restore.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.databinding.ActivityWalletBackupBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionFragment
import javax.inject.Inject

class WalletRestoreActivity : CommonActivity<ActivityWalletBackupBinding, WalletRestoreViewModel>() {

    @Inject
    lateinit var prefs: SharedPrefsRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var migrationManager: MigrationManager

    @Inject
    lateinit var tariSettingsRepository: TariSettingsSharedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityWalletBackupBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel : WalletRestoreViewModel by viewModels()
        bindViewModel(viewModel)
        
        setContainerId(R.id.backup_fragment_container)

        if (savedInstanceState == null) {
            addFragment(ChooseRestoreOptionFragment(), isRoot = true)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_top, R.anim.exit_to_bottom)
    }

    override fun onDestroy() {
        if (!tariSettingsSharedRepository.isRestoredWallet) {
            walletServiceLauncher.stop()
        }
        super.onDestroy()
    }

    companion object {
        fun navigationIntent(context: Context) = Intent(context, WalletRestoreActivity::class.java)
    }
}
