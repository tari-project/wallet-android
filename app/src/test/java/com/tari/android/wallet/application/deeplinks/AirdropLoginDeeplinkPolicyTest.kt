package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.application.deeplinks.AirdropLoginDeeplinkPolicy.Decision
import com.tari.android.wallet.application.deeplinks.AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirdropLoginDeeplinkPolicyTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `fresh marker saves silently`() {
        assertEquals(Decision.SAVE, AirdropLoginDeeplinkPolicy.decide(now - 1000, now, "token", "refresh"))
        assertEquals(Decision.SAVE, AirdropLoginDeeplinkPolicy.decide(now, now, "token", "refresh"))
        assertEquals(Decision.SAVE, AirdropLoginDeeplinkPolicy.decide(now - PENDING_LOGIN_TTL_MS + 1, now, "token", "refresh"))
    }

    @Test
    fun `ttl is ten minutes`() {
        assertEquals(10 * 60 * 1000L, PENDING_LOGIN_TTL_MS)
        assertEquals(Decision.CONFIRM, AirdropLoginDeeplinkPolicy.decide(now - 11 * 60 * 1000L, now, "token", "refresh"))
    }

    @Test
    fun `expired marker asks for confirmation`() {
        assertEquals(Decision.CONFIRM, AirdropLoginDeeplinkPolicy.decide(now - PENDING_LOGIN_TTL_MS, now, "token", "refresh"))
        assertEquals(Decision.CONFIRM, AirdropLoginDeeplinkPolicy.decide(now - 2 * PENDING_LOGIN_TTL_MS, now, "token", "refresh"))
    }

    @Test
    fun `marker from the future asks for confirmation`() {
        assertEquals(Decision.CONFIRM, AirdropLoginDeeplinkPolicy.decide(now + 1000, now, "token", "refresh"))
    }

    @Test
    fun `no marker asks for confirmation`() {
        assertEquals(Decision.CONFIRM, AirdropLoginDeeplinkPolicy.decide(null, now, "token", "refresh"))
    }

    @Test
    fun `empty token or refresh token is ignored`() {
        assertEquals(Decision.IGNORE, AirdropLoginDeeplinkPolicy.decide(now, now, "", "refresh"))
        assertEquals(Decision.IGNORE, AirdropLoginDeeplinkPolicy.decide(now, now, "token", ""))
        assertEquals(Decision.IGNORE, AirdropLoginDeeplinkPolicy.decide(now, now, " ", "refresh"))
        assertEquals(Decision.IGNORE, AirdropLoginDeeplinkPolicy.decide(null, now, "", ""))
    }

    @Test
    fun `confirmation is pending only within the window`() {
        assertEquals(false, AirdropLoginDeeplinkPolicy.isConfirmationPending(null, now))
        assertEquals(true, AirdropLoginDeeplinkPolicy.isConfirmationPending(now, now))
        assertEquals(true, AirdropLoginDeeplinkPolicy.isConfirmationPending(now - PENDING_LOGIN_TTL_MS + 1, now))
        assertEquals(false, AirdropLoginDeeplinkPolicy.isConfirmationPending(now - PENDING_LOGIN_TTL_MS, now))
        assertEquals(false, AirdropLoginDeeplinkPolicy.isConfirmationPending(now + 1000, now))
    }

    @Test
    fun `account name keeps plain names`() {
        assertEquals("Alice", AirdropLoginDeeplinkPolicy.sanitizeAccountName("Alice"))
        assertEquals("Alice Smith", AirdropLoginDeeplinkPolicy.sanitizeAccountName("  Alice   Smith "))
    }

    @Test
    fun `account name turns line breaks into single spaces`() {
        assertEquals("Alice Use this account", AirdropLoginDeeplinkPolicy.sanitizeAccountName("Alice\n\nUse this account\r\n"))
        assertEquals("Alice Bob", AirdropLoginDeeplinkPolicy.sanitizeAccountName("Alice\u2028Bob"))
    }

    @Test
    fun `account name drops bidi overrides and zero width characters`() {
        assertEquals("evilgnp.exe", AirdropLoginDeeplinkPolicy.sanitizeAccountName("evil\u202Egnp.exe"))
        assertEquals("ab", AirdropLoginDeeplinkPolicy.sanitizeAccountName("a\u2066\u2069b"))
        assertEquals("Alice", AirdropLoginDeeplinkPolicy.sanitizeAccountName("Al\u200Bi\u200Dce\uFEFF"))
        assertEquals("ab", AirdropLoginDeeplinkPolicy.sanitizeAccountName("a\u0007\u0000b"))
    }

    @Test
    fun `long account name is truncated with an ellipsis`() {
        val name = AirdropLoginDeeplinkPolicy.sanitizeAccountName("a".repeat(100))!!
        assertEquals(AirdropLoginDeeplinkPolicy.MAX_ACCOUNT_NAME_LENGTH, name.length)
        assertEquals("a".repeat(AirdropLoginDeeplinkPolicy.MAX_ACCOUNT_NAME_LENGTH - 1) + "\u2026", name)
        assertEquals("a".repeat(64), AirdropLoginDeeplinkPolicy.sanitizeAccountName("a".repeat(64)))
    }

    @Test
    fun `truncation does not split an emoji`() {
        val name = AirdropLoginDeeplinkPolicy.sanitizeAccountName("a".repeat(62) + "\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00")!!
        assertEquals("a".repeat(62) + "\uD83D\uDE00\u2026", name)
    }

    @Test
    fun `account name with only control or format characters is rejected`() {
        assertNull(AirdropLoginDeeplinkPolicy.sanitizeAccountName("\u202E\u200B\n\t "))
        assertNull(AirdropLoginDeeplinkPolicy.sanitizeAccountName(""))
    }
}
