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
package com.tari.android.wallet.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.util.Constants.UI.Splash
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject

/**
 * Splash screen activity.
 *
 * @author The Tari Development Team
 */
internal class SplashActivity : CommonActivity() {

    @Inject
    internal lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    internal lateinit var storage: YatUserStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed(Splash.createWalletStartUpDelayMs) {
            val exists =
                WalletUtil.walletExists(applicationContext)
                        && sharedPrefsWrapper.onboardingAuthSetupCompleted
                        && storage.get()?.emojiIds?.firstOrNull() != null
            launch(if (exists) AuthActivity::class.java else OnboardingFlowActivity::class.java)
        }
    }

    private fun <T : Activity> launch(destination: Class<T>) {
        val intent = Intent(this, destination)
        this.intent.data?.let(intent::setData)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // no-op
    }

}
