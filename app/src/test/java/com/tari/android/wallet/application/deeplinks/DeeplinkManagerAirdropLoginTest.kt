package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.dialog.modular.IDialogModule
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeeplinkManagerAirdropLoginTest {

    private var pendingSince: Long? = null
    private var dialogModules: List<IDialogModule> = emptyList()

    private val airdropRepository = mockk<AirdropRepository>(relaxed = true) {
        every { airdropToken } returns MutableStateFlow(null)
        every { getLoginPendingSince() } answers { pendingSince }
        every { clearLoginPending() } answers { pendingSince = null }
        coEvery { getAccountName(any()) } returns "alice"
    }
    private val dialogHandler = mockk<DialogHandler>(relaxed = true) {
        every { showModularDialog(DialogId.DEEPLINK_AIRDROP_LOGIN, *varargAll { true }) } answers {
            @Suppress("UNCHECKED_CAST")
            dialogModules = (args[1] as Array<IDialogModule>).toList()
        }
    }
    private val deeplinkManager = DeeplinkManager(
        contactRepository = mockk(relaxed = true),
        resourceManager = mockk(relaxed = true) {
            every { getString(any()) } returns ""
            every { getString(R.string.airdrop_login_deeplink_unverified) } returns "unverified"
            every { getString(R.string.airdrop_login_deeplink_account, *anyVararg()) } answers { "Account: " + (args[1] as Array<*>).joinToString() }
        },
        walletManager = mockk(relaxed = true),
        navigator = mockk(relaxed = true),
        deeplinkParser = mockk(relaxed = true),
        airdropRepository = airdropRepository,
        applicationScope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun dialogBody(): String = dialogModules.filterIsInstance<BodyModule>().first().text.orEmpty()

    private fun confirmDialog() = dialogModules.filterIsInstance<ButtonModule>().first().action()

    @Test
    fun `fresh marker saves silently, is consumed and shows connected message`() {
        pendingSince = System.currentTimeMillis()

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))

        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
        verify(exactly = 0) { dialogHandler.showModularDialog(any<Int>(), *anyVararg()) }
        verify(exactly = 1) { dialogHandler.showSimpleDialog(any(), any<CharSequence>(), any<CharSequence>(), any(), any(), any(), any()) }
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
    fun `no marker shows the account and saves only after confirm`() {
        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))

        assertTrue(dialogBody().contains("Account: alice"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }

        confirmDialog()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
    }

    @Test
    fun `candidate token is only used for the lookup before confirm`() {
        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("candidate", "candidate-refresh"))

        coVerify(exactly = 1) { airdropRepository.getAccountName("candidate") }
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        verify(exactly = 0) { airdropRepository.clearAirdropToken() }
    }

    @Test
    fun `failed account lookup still shows dialog marked unverified`() {
        coEvery { airdropRepository.getAccountName(any()) } returns null

        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken("token", "refresh"))

        assertTrue(dialogBody().contains("unverified"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }

        confirmDialog()
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
        coVerify(exactly = 0) { airdropRepository.getAccountName(any()) }
        verify(exactly = 0) { dialogHandler.showModularDialog(any<Int>(), *anyVararg()) }
        assertEquals(marker, pendingSince)
    }
}
