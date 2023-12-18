package com.tari.android.wallet.infrastructure.security.biometric

import androidx.biometric.BiometricPrompt
import java.util.concurrent.atomic.AtomicBoolean

class SingleSuccessOrErrorAdapterDecorator(private val decorated: BiometricPrompt.AuthenticationCallback) :
    BiometricPrompt.AuthenticationCallback() {

    private val isResumed = AtomicBoolean(false)

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        if (!isResumed.get()) {
            if (result.cryptoObject?.cipher == null) {
                decorated.onAuthenticationSucceeded(result)
                isResumed.set(true)
            } else {
                decorated.onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "Crypto object is not null")
                isResumed.set(true)
            }
        }
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (!isResumed.get()) {
            decorated.onAuthenticationError(errorCode, errString)
            isResumed.set(true)
        }
    }
}