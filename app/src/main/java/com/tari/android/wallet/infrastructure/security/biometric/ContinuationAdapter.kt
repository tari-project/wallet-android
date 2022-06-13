package com.tari.android.wallet.infrastructure.security.biometric

import androidx.biometric.BiometricPrompt
import kotlin.coroutines.Continuation

class ContinuationAdapter(private val continuation: Continuation<Boolean>) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        continuation.resumeWith(Result.success(true))
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        continuation.resumeWith(Result.failure(BiometricAuthenticationException(errorCode, errString.toString())))
    }
}