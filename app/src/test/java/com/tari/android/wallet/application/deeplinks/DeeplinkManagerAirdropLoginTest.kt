package com.tari.android.wallet.application.deeplinks

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.airdrop.VerifiedAirdropAccount
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SuppressLint("RestrictedApi")
class DeeplinkManagerAirdropLoginTest {

    private var pendingSince: Long? = null
    private val airdropTokenFlow = MutableStateFlow<String?>(null)

    private val airdropRepository = mockk<AirdropRepository>(relaxed = true) {
        every { airdropToken } returns airdropTokenFlow
        every { getLoginPendingSince() } answers { pendingSince }
        every { clearLoginPending() } answers { pendingSince = null }
        coEvery { verifyAccount(any()) } returns VerifiedAirdropAccount("new-token", "new-refresh", "alice")
    }
    private val dialogHandler = mockk<DialogHandler>(relaxed = true)
    private val dialogManager = mockk<DialogManager>(relaxed = true)
    private var now = 1_700_000_000_000L
    private val deeplinkManager = DeeplinkManager(
        contactRepository = mockk(relaxed = true),
        resourceManager = mockk(relaxed = true) {
            every { getString(any()) } returns ""
            every { getString(R.string.airdrop_login_deeplink_unverified) } returns "unverified"
            every { getString(R.string.airdrop_login_deeplink_replace_warning) } returns "WARNING"
            every { getString(R.string.airdrop_login_deeplink_account, *anyVararg()) } answers { "Account: " + (args[1] as Array<*>).joinToString() }
        },
        walletManager = mockk(relaxed = true),
        navigator = mockk(relaxed = true),
        deeplinkParser = mockk(relaxed = true),
        airdropRepository = airdropRepository,
        dialogManager = dialogManager,
        applicationScope = CoroutineScope(Dispatchers.Unconfined),
        mainDispatcher = Dispatchers.Unconfined,
    ).apply { currentTimeMillis = { now } }

    @Before
    fun setUp() {
        // Runs LiveData posts synchronously on the test thread
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private fun shownDialog(): ModularDialogArgs? = deeplinkManager.airdropLoginDialog.value

    private fun ModularDialogArgs.body(): String = modules.filterIsInstance<BodyModule>().first().text.orEmpty()

    // Mirrors ModularDialog: a Normal button runs its action; Cancel (Close) runs its action and then dismisses
    private fun ModularDialogArgs.tapMainButton() = modules.filterIsInstance<ButtonModule>().first { it.style == ButtonStyle.Normal }.action()

    private fun ModularDialogArgs.tapCancel() {
        modules.filterIsInstance<ButtonModule>().first { it.style == ButtonStyle.Close }.action()
        dismiss()
    }

    private fun ModularDialogArgs.dismiss() = dialogArgs.onDismiss()

    private fun openLink(token: String = "token", refreshToken: String = "refresh") =
        deeplinkManager.execute(dialogHandler, DeepLink.AirdropLoginToken(token, refreshToken))

    @Test
    fun `fresh marker saves silently, is consumed and shows connected message`() {
        pendingSince = now

        openLink()

        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
        verify(exactly = 1) { dialogHandler.showSimpleDialog(any(), any<CharSequence>(), any<CharSequence>(), any(), any(), any(), any()) }
        assertNull(shownDialog())
        assertNull(pendingSince)
    }

    @Test
    fun `no marker shows warning dialog without any network call`() {
        openLink()

        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shownDialog()!!.dialogId)
        coVerify(exactly = 0) { airdropRepository.verifyAccount(any()) }
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `show account verifies the refresh token and confirm saves the refreshed pair`() {
        openLink("candidate", "candidate-refresh")
        val warning = shownDialog()!!
        assertTrue(warning.filterObscuredTouches)
        warning.tapMainButton()
        warning.dismiss()

        coVerify(exactly = 1) { airdropRepository.verifyAccount("candidate-refresh") }
        val account = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT, account.dialogId)
        assertTrue(account.filterObscuredTouches)
        assertTrue(account.body().contains("Account: alice"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        verify(exactly = 0) { airdropRepository.clearAirdropToken() }

        account.tapMainButton()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("new-token", "new-refresh") }
        verify(exactly = 0) { airdropRepository.saveAirdropToken("candidate", "candidate-refresh") }
    }

    @Test
    fun `cancel on account dialog does not save`() {
        openLink()
        shownDialog()!!.tapMainButton()

        shownDialog()!!.tapCancel()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `failed verification shows unverified account dialog that can only be cancelled`() {
        // Covers refresh failure, details failure and a blank name: verifyAccount returns null for all of them
        coEvery { airdropRepository.verifyAccount(any()) } returns null

        openLink()
        shownDialog()!!.tapMainButton()

        val account = shownDialog()!!
        assertTrue(account.body().contains("unverified"))
        assertEquals(listOf(ButtonStyle.Close), account.modules.filterIsInstance<ButtonModule>().map { it.style })
        account.tapCancel()
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `second link is ignored while warning dialog is pending`() {
        openLink()
        val warning = shownDialog()

        openLink("attacker", "attacker-refresh")

        assertSame(warning, shownDialog())
    }

    @Test
    fun `second link is ignored while account dialog is pending`() {
        openLink()
        val warning = shownDialog()!!
        warning.tapMainButton()
        warning.dismiss()
        val account = shownDialog()

        openLink("attacker", "attacker-refresh")

        assertSame(account, shownDialog())
        coVerify(exactly = 0) { airdropRepository.verifyAccount("attacker-refresh") }
    }

    @Test
    fun `pending is cleared when warning dialog is dismissed`() {
        openLink()
        val warning = shownDialog()!!
        warning.dismiss()

        openLink("next", "next-refresh")

        val next = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, next.dialogId)
        next.tapMainButton()
        coVerify(exactly = 1) { airdropRepository.verifyAccount("next-refresh") }
    }

    @Test
    fun `pending is cleared after cancel on warning dialog`() {
        openLink()
        val warning = shownDialog()!!
        warning.tapCancel()

        openLink("next", "next-refresh")

        assertTrue(warning !== shownDialog())
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shownDialog()!!.dialogId)
    }

    @Test
    fun `pending is cleared after confirm or cancel on account dialog`() {
        openLink()
        shownDialog()!!.tapMainButton()
        val confirmed = shownDialog()!!
        confirmed.tapMainButton()
        confirmed.dismiss()

        openLink("second", "second-refresh")
        val secondWarning = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, secondWarning.dialogId)
        secondWarning.tapMainButton()
        shownDialog()!!.tapCancel()

        openLink("third", "third-refresh")
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shownDialog()!!.dialogId)
        verify(exactly = 0) { airdropRepository.saveAirdropToken("second", any()) }
    }

    @Test
    fun `late dismiss of an earlier confirmation does not clear the current one`() {
        openLink()
        val first = shownDialog()!!
        first.dismiss()
        openLink("second", "second-refresh")
        val second = shownDialog()

        first.dismiss()
        openLink("attacker", "attacker-refresh")

        assertSame(second, shownDialog())
    }

    @Test
    fun `expired pending confirmation lets a new link start over and closes the stale dialogs`() {
        openLink()
        val stale = shownDialog()

        every { dialogManager.isDialogShowing(any()) } returns true
        now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
        openLink("next", "next-refresh")

        assertTrue(stale !== shownDialog())
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shownDialog()!!.dialogId)
        verify(exactly = 1) { dialogManager.dismiss(DialogId.DEEPLINK_AIRDROP_LOGIN, true) }
        verify(exactly = 1) { dialogManager.dismiss(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT, true) }
    }

    @Test
    fun `link within the pending window is ignored`() {
        openLink()
        val first = shownDialog()

        now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS - 1
        openLink("attacker", "attacker-refresh")

        assertSame(first, shownDialog())
        verify(exactly = 0) { dialogManager.dismiss(any(), true) }
    }

    @Test
    fun `lookup finishing after the session changed shows nothing`() {
        openLink("old", "old-refresh")
        val oldWarning = shownDialog()!!
        coEvery { airdropRepository.verifyAccount("old-refresh") } answers {
            now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
            openLink("new", "new-refresh")
            VerifiedAirdropAccount("old-new-token", "old-new-refresh", "old account")
        }

        oldWarning.tapMainButton()

        val shown = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shown.dialogId)
        assertTrue(shown !== oldWarning)
    }

    @Test
    fun `confirm on an account dialog from an older session does not save`() {
        openLink("old", "old-refresh")
        shownDialog()!!.tapMainButton()
        val oldAccount = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT, oldAccount.dialogId)

        now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
        openLink("new", "new-refresh")
        oldAccount.tapMainButton()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `show account on a warning dialog from an older session does nothing`() {
        openLink("old", "old-refresh")
        val oldWarning = shownDialog()!!

        now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
        openLink("new", "new-refresh")
        val newWarning = shownDialog()
        oldWarning.tapMainButton()

        coVerify(exactly = 0) { airdropRepository.verifyAccount(any()) }
        assertSame(newWarning, shownDialog())
    }

    @Test
    fun `own sign-in cancels an open confirmation`() {
        openLink("attacker", "attacker-refresh")
        shownDialog()!!.tapMainButton()
        val attackerAccount = shownDialog()!!
        every { dialogManager.isDialogShowing(any()) } returns true

        pendingSince = now
        openLink("own", "own-refresh")
        attackerAccount.tapMainButton()

        verify(exactly = 1) { airdropRepository.saveAirdropToken("own", "own-refresh") }
        verify(exactly = 0) { airdropRepository.saveAirdropToken("new-token", "new-refresh") }
        verify(exactly = 1) { dialogManager.dismiss(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT, true) }
    }

    @Test
    fun `cancelling the confirmation makes its dialogs inert and lets a new link show`() {
        openLink("attacker", "attacker-refresh")
        val attackerWarning = shownDialog()!!

        deeplinkManager.cancelAirdropLoginConfirmation()
        attackerWarning.tapMainButton()

        coVerify(exactly = 0) { airdropRepository.verifyAccount(any()) }
        openLink("next", "next-refresh")
        assertTrue(attackerWarning !== shownDialog())
    }

    @Test
    fun `cancelling after the account is shown makes confirm do nothing`() {
        openLink()
        shownDialog()!!.tapMainButton()
        val account = shownDialog()!!

        deeplinkManager.cancelAirdropLoginConfirmation()
        account.tapMainButton()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `confirm is refused once the pending window has expired`() {
        openLink()
        shownDialog()!!.tapMainButton()
        val account = shownDialog()!!

        now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
        account.tapMainButton()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `confirm is refused when a session appeared after the dialog was built`() {
        openLink()
        shownDialog()!!.tapMainButton()
        val account = shownDialog()!!

        airdropTokenFlow.value = "existing"
        account.tapMainButton()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `confirm is refused when the session went away after the dialog was built`() {
        airdropTokenFlow.value = "existing"
        openLink()
        shownDialog()!!.tapMainButton()
        val account = shownDialog()!!

        airdropTokenFlow.value = null
        account.tapMainButton()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `replace warning is shown above the account name`() {
        airdropTokenFlow.value = "existing"
        openLink()
        shownDialog()!!.tapMainButton()

        val body = shownDialog()!!.body()
        assertTrue(body.startsWith("WARNING"))
        assertTrue(body.indexOf("WARNING") < body.indexOf("Account: alice"))

        shownDialog()!!.tapMainButton()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("new-token", "new-refresh") }
    }

    @Test
    fun `expired marker is cleared and needs confirmation`() {
        pendingSince = now - AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS - 1

        openLink()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN, shownDialog()!!.dialogId)
        assertNull(pendingSince)
    }

    @Test
    fun `empty token is ignored and keeps the marker`() {
        val marker = now
        pendingSince = marker

        openLink("", "refresh")

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        assertNull(shownDialog())
        assertEquals(marker, pendingSince)
    }
}
