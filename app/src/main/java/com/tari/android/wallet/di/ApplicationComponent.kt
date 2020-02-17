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
package com.tari.android.wallet.di

import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.AuthActivity
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.activity.QRScannerActivity
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.activity.log.DebugLogActivity
import com.tari.android.wallet.ui.activity.onboarding.OnBoardingFlowActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.fragment.onboarding.CreateEmojiIdFragment
import com.tari.android.wallet.ui.fragment.onboarding.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.send.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.AddNoteAndSendFragment
import com.tari.android.wallet.ui.fragment.send.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.SendTxSuccessfulFragment
import dagger.Component
import javax.inject.Singleton

/**
 * Dagger component that injects objects through modules.
 *
 * @author The Tari Development Team
 */
@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        WalletModule::class,
        RestModule::class,
        ConfigModule::class
    ]
)
interface ApplicationComponent {

    /**
     * Application.
     */
    fun inject(appplication: TariWalletApplication)

    /**
     * Activities.
     */
    fun inject(activity: SplashActivity)
    fun inject(activity: OnBoardingFlowActivity)
    fun inject(activity: AuthActivity)
    fun inject(activity: HomeActivity)
    fun inject(activity: QRScannerActivity)
    fun inject(activity: DebugLogActivity)
    fun inject(activity: SendTariActivity)

    /**
     * Fragments.
     */
    fun inject(fragment: CreateWalletFragment)
    fun inject(fragment: CreateEmojiIdFragment)
    fun inject(fragment: AddRecipientFragment)
    fun inject(fragment: AddAmountFragment)
    fun inject(fragment: AddNoteAndSendFragment)
    fun inject(fragment: SendTxSuccessfulFragment)

    /**
     * Service(s).
     */
    fun inject(service: WalletService)

}