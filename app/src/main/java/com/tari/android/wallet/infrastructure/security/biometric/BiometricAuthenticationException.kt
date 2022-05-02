package com.tari.android.wallet.infrastructure.security.biometric

class BiometricAuthenticationException(val code: Int, message: String?) : RuntimeException(message)