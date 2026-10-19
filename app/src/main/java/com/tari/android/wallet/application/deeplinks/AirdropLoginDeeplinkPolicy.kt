package com.tari.android.wallet.application.deeplinks

object AirdropLoginDeeplinkPolicy {

    const val PENDING_LOGIN_TTL_MS = 10 * 60 * 1000L

    enum class Decision { SAVE, CONFIRM, IGNORE }

    /**
     * Save silently only if the user started the login from this app within the TTL; otherwise ask the user to confirm.
     */
    fun decide(pendingSince: Long?, now: Long, token: String, refreshToken: String): Decision {
        if (token.isBlank() || refreshToken.isBlank()) return Decision.IGNORE
        if (pendingSince == null) return Decision.CONFIRM
        val age = now - pendingSince
        return if (age in 0 until PENDING_LOGIN_TTL_MS) Decision.SAVE else Decision.CONFIRM
    }

    /**
     * A confirmation dialog can disappear without a dismiss callback (e.g. its activity is destroyed), so a pending one only blocks new links
     * for a bounded time.
     */
    fun isConfirmationPending(pendingSince: Long?, now: Long): Boolean {
        if (pendingSince == null) return false
        val age = now - pendingSince
        return age in 0 until PENDING_LOGIN_TTL_MS
    }
}
