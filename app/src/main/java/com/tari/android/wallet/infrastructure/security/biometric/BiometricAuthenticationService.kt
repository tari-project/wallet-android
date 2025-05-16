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
package com.tari.android.wallet.infrastructure.security.biometric

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.R
import com.tari.android.wallet.util.extension.isTrue
import java.util.concurrent.Executor
import kotlin.coroutines.suspendCoroutine

class BiometricAuthenticationService constructor(
    private val executor: Executor,
    private val manager: BiometricManager,
    private val keyguardManager: KeyguardManager?
) {
    val isDeviceSecured: Boolean
        get() = keyguardManager?.isDeviceSecure.isTrue()

    val authType: BiometricAuthenticationType
        get() = when {
            isBiometricAuthAvailable -> BiometricAuthenticationType.BIOMETRIC
            isDeviceSecured -> BiometricAuthenticationType.PIN
            else -> BiometricAuthenticationType.NONE
        }

    val isBiometricAuthAvailable: Boolean
        get() = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(fragment: Fragment, title: CharSequence, subtitle: CharSequence): Boolean = authenticate(
        fragment.requireContext(),
        { BiometricPrompt(fragment, executor, it) },
        title,
        subtitle
    )

    suspend fun authenticate(activity: FragmentActivity, title: CharSequence, subtitle: CharSequence): Boolean = authenticate(
        activity,
        { BiometricPrompt(activity, executor, it) },
        title,
        subtitle
    )

    private suspend fun authenticate(
        context: Context,
        provider: (BiometricPrompt.AuthenticationCallback) -> BiometricPrompt,
        title: CharSequence,
        subtitle: CharSequence
    ): Boolean = suspendCoroutine {
        provider(SingleSuccessOrErrorAdapterDecorator(ContinuationAdapter(it))).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(context.getString(R.string.common_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }
}