package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class DeeplinkManagerAirdropLoginTest {

    private var pendingSince: Long? = null

    private val airdropRepository = mockk<AirdropRepository>(relaxed = true) {
        every { airdropToken } returns MutableStateFlow(null)
        every { getLoginPendingSince() } answers { pendingSince }
        every { clearLoginPending() } answers { pendingSince = null }
    }
    private val dialogHandler = mockk<DialogHandler>(relaxed = true)
    private val deeplinkManager = DeeplinkManager(
        contactRepository = mockk(relaxed = true),
        resourceManager = mockk(relaxed = true) { every { getString(any()) } returns "" },
        walletManager = mockk(relaxed = true),
        navigator = mockk(relaxed = true),
        deeplinkParser = mockk(relaxed = true),
        airdropRepository = airdropRepository,
        applicationScope = mockk(relaxed = true),
    )

    @Test
    fun `fresh marker saves silently and is consumed`() {
        pendingSince = System.currentTimeMillis()

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))

        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
        verify(exactly = 0) { dialogHandler.showModularDialog(any<Int>(), *anyVararg()) }
        assertEquals(null, pendingSince)
    }

    @Test
    fun `second deeplink after marker is consumed needs confirmation`() {
        pendingSince = System.currentTimeMillis()

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))
        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("attacker", "attacker-refresh"))

        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
        verify(exactly = 0) { airdropRepository.saveAirdropToken("attacker", "attacker-refresh") }
        verify(exactly = 1) { dialogHandler.showModularDialog(DialogId.DEEPLINK_AIRDROP_LOGIN, *anyVararg()) }
    }

    @Test
    fun `no marker saves only after confirm`() {
        val modules = slot<Array<IDialogModule>>()
        every { dialogHandler.showModularDialog(DialogId.DEEPLINK_AIRDROP_LOGIN, *varargAll { true }) } answers {
            @Suppress("UNCHECKED_CAST")
            modules.captured = (args[1] as Array<IDialogModule>)
        }

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }

        modules.captured.filterIsInstance<ButtonModule>().first().action()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
    }

    @Test
    fun `expired marker is cleared and needs confirmation`() {
        pendingSince = System.currentTimeMillis() - AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS - 1

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        verify(exactly = 1) { dialogHandler.showModularDialog(DialogId.DEEPLINK_AIRDROP_LOGIN, *anyVararg()) }
        assertEquals(null, pendingSince)
    }

    @Test
    fun `empty token is ignored and keeps the marker`() {
        val marker = System.currentTimeMillis()
        pendingSince = marker

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("", "refresh"))

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        verify(exactly = 0) { dialogHandler.showModularDialog(any<Int>(), *anyVararg()) }
        assertEquals(marker, pendingSince)
    }
}
