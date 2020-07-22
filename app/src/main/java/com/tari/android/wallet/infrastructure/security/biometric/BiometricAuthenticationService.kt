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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class BiometricAuthenticationService(
    private val executor: Executor,
    private val manager: BiometricManager,
    private val keyguardManager: KeyguardManager?
) {

    enum class BiometricAuthenticationType {
        BIOMETRIC,
        PIN,
        NONE
    }

    class BiometricAuthenticationException(val code: Int, message: String?) :
        RuntimeException(message)

    val isBiometricAuthAvailable: Boolean
        get() = manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

    val isDeviceSecured: Boolean
        get() = keyguardManager?.isDeviceSecure ?: false

    suspend fun authenticate(
        fragment: Fragment,
        title: CharSequence,
        subtitle: CharSequence,
        deviceCredentialsAllowed: Boolean = true
    ): Boolean = authenticate(
        { BiometricPrompt(fragment, executor, it) },
        title,
        subtitle,
        deviceCredentialsAllowed
    )

    suspend fun authenticate(
        activity: FragmentActivity,
        title: CharSequence,
        subtitle: CharSequence,
        deviceCredentialsAllowed: Boolean = true
    ): Boolean = authenticate(
        { BiometricPrompt(activity, executor, it) },
        title,
        subtitle,
        deviceCredentialsAllowed
    )

    private suspend fun authenticate(
        provider: (BiometricPrompt.AuthenticationCallback) -> BiometricPrompt,
        title: CharSequence,
        subtitle: CharSequence,
        deviceCredentialsAllowed: Boolean = true
    ): Boolean = suspendCoroutine {
        provider(SingleSuccessOrErrorAdapterDecorator(ContinuationAdapter(it))).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDeviceCredentialAllowed(deviceCredentialsAllowed)
                .build()
        )
    }

    private class ContinuationAdapter(private val continuation: Continuation<Boolean>) :
        BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            continuation.resumeWith(Result.success(true))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            continuation.resumeWith(
                Result.failure(BiometricAuthenticationException(errorCode, errString.toString()))
            )
        }
    }

    private class SingleSuccessOrErrorAdapterDecorator(
        private val decorated: BiometricPrompt.AuthenticationCallback
    ) : BiometricPrompt.AuthenticationCallback() {

        private val isResumed = AtomicBoolean(false)

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (!isResumed.get()) {
                decorated.onAuthenticationSucceeded(result)
                isResumed.set(true)
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!isResumed.get()) {
                decorated.onAuthenticationError(errorCode, errString)
                isResumed.set(true)
            }
        }
    }

}
