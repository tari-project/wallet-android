package com.tari.android.wallet.application.deeplinks

object AirdropLoginDeeplinkPolicy {

    const val PENDING_LOGIN_TTL_MS = 10 * 60 * 1000L
    const val MAX_ACCOUNT_NAME_LENGTH = 64

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

    /**
     * Makes a server-provided account name safe to show: whitespace (including line breaks) becomes single spaces, control and format
     * characters (bidi overrides, zero-width characters) are removed and the length is capped. Returns null if nothing is left.
     */
    fun sanitizeAccountName(name: String): String? {
        val cleaned = StringBuilder()
        var offset = 0
        while (offset < name.length) {
            val codePoint = name.codePointAt(offset)
            offset += Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                cleaned.append(' ')
            } else if (type != Character.CONTROL.toInt() && type != Character.FORMAT.toInt() && type != Character.SURROGATE.toInt()) {
                cleaned.appendCodePoint(codePoint)
            }
        }
        val collapsed = cleaned.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
        if (collapsed.isEmpty()) return null
        if (collapsed.codePointCount(0, collapsed.length) <= MAX_ACCOUNT_NAME_LENGTH) return collapsed
        val end = collapsed.offsetByCodePoints(0, MAX_ACCOUNT_NAME_LENGTH - 1)
        return collapsed.substring(0, end).trimEnd() + "\u2026"
    }
}
