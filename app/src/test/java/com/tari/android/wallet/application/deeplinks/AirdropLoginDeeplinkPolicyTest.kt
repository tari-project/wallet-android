package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.application.deeplinks.AirdropLoginDeeplinkPolicy.Decision
import com.tari.android.wallet.application.deeplinks.AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
import org.junit.Assert.assertEquals
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
}
