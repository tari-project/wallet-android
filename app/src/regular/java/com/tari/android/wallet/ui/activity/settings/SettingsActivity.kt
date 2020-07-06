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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.fragment.settings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.*

class SettingsActivity : AppCompatActivity(), SettingsRouter {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_top)
        if (savedInstanceState == null) {
            loadFragment()
        }
    }

    private fun loadFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.settings_fragment_container, AllSettingsFragment.newInstance())
            .commit()
    }

    override fun toWalletBackupSettings() {
        addFragment(WalletBackupSettingsFragment.newInstance())
    }

    override fun toWalletBackupWithRecoveryPhrase() {
        addFragment(WriteDownSeedPhraseFragment.newInstance())
    }

    override fun toRecoveryPhraseVerification(phrase: List<String>) {
        addFragment(VerifySeedPhraseFragment.newInstance(phrase))
    }

    override fun toConfirmPassword() {
        addFragment(EnterCurrentPasswordFragment.newInstance())
    }

    override fun toChangePassword() {
        addFragment(ChangeSecurePasswordFragment.newInstance())
    }

    override fun onPasswordChanged() {
        if (supportFragmentManager
                .findFragmentByTag(EnterCurrentPasswordFragment::class.java.simpleName) != null
        ) {
            supportFragmentManager.popBackStackImmediate(
                EnterCurrentPasswordFragment::class.java.simpleName,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            val fragments = supportFragmentManager.fragments
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.no_anim, R.anim.no_anim)
                .apply { fragments.subList(0, fragments.size - 2).forEach { hide(it) } }
                .commit()
        } else {
            onBackPressed()
        }
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .apply { supportFragmentManager.fragments.forEach { hide(it) } }
            .add(R.id.settings_fragment_container, fragment, fragment.javaClass.simpleName)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // On back press all transitive fragments become visible for some reason, and when
        // navigating back from ChangeSecurePasswordFragment then AllSettingsFragment becomes
        // visible as well, so we hiding all the transitive fragments except for the one that
        // becomes the topmost by force
        overridePendingTransition(R.anim.enter_from_top, R.anim.exit_to_bottom)
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 1) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.no_anim, R.anim.no_anim)
                .apply { fragments.subList(0, fragments.size - 2).forEach { hide(it) } }
                .commit()
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

}
