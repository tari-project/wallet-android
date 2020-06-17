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
package com.tari.android.wallet.ui.activity.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.activity.AuthActivity
import com.tari.android.wallet.ui.extension.backupAndRestoreComponent
import com.tari.android.wallet.ui.fragment.restore.ChooseRestoreOptionFragment
import com.tari.android.wallet.ui.fragment.restore.RestorationWithCloudFragment
import com.tari.android.wallet.ui.fragment.restore.WalletRestoringFragment
import com.tari.android.wallet.util.SharedPrefsWrapper
import javax.inject.Inject

class WalletRestoreActivity : AppCompatActivity(), WalletRestoreRouter {

    @Inject
    lateinit var prefs: SharedPrefsWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        backupAndRestoreComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_backup)
        if (savedInstanceState == null) {
            loadChooseBackupOptionFragment()
        }
    }

    private fun loadChooseBackupOptionFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.backup_fragment_container, ChooseRestoreOptionFragment.newInstance())
            .commit()
    }

    override fun toBackupWithCloud() {
        loadFragment(RestorationWithCloudFragment.newInstance())
    }

    override fun toBackupWithRecoveryPhrase() {
        loadFragment(WalletRestoringFragment.newInstance())
    }

    override fun onBackupCompleted() {
        // wallet restored, setup shared prefs accordingly
        prefs.onboardingCompleted = true
        prefs.onboardingAuthSetupCompleted = true
        prefs.onboardingDisplayedAtHome = true

        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .apply { supportFragmentManager.fragments.forEach { hide(it) } }
            .add(R.id.backup_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun navigationIntent(context: Context) = Intent(context, WalletRestoreActivity::class.java)
    }

}
