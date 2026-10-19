package com.tari.android.wallet.application.deeplinks

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.AirdropRepository
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

    private val airdropRepository = mockk<AirdropRepository>(relaxed = true) {
        every { airdropToken } returns MutableStateFlow(null)
        every { getLoginPendingSince() } answers { pendingSince }
        every { clearLoginPending() } answers { pendingSince = null }
        coEvery { getAccountName(any()) } returns "alice"
    }
    private val dialogHandler = mockk<DialogHandler>(relaxed = true)
    private val dialogManager = mockk<DialogManager>(relaxed = true)
    private var now = 1_700_000_000_000L
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
        coVerify(exactly = 0) { airdropRepository.getAccountName(any()) }
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `show account looks up candidate token and confirm saves`() {
        openLink("candidate", "candidate-refresh")
        val warning = shownDialog()!!
        warning.tapMainButton()
        warning.dismiss()

        coVerify(exactly = 1) { airdropRepository.getAccountName("candidate") }
        val account = shownDialog()!!
        assertEquals(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT, account.dialogId)
        assertTrue(account.body().contains("Account: alice"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
        verify(exactly = 0) { airdropRepository.clearAirdropToken() }

        account.tapMainButton()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("candidate", "candidate-refresh") }
    }

    @Test
    fun `cancel on account dialog does not save`() {
        openLink()
        shownDialog()!!.tapMainButton()

        shownDialog()!!.tapCancel()

        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }
    }

    @Test
    fun `failed lookup shows unverified account dialog and saves only on confirm`() {
        coEvery { airdropRepository.getAccountName(any()) } returns null

        openLink()
        shownDialog()!!.tapMainButton()

        val account = shownDialog()!!
        assertTrue(account.body().contains("unverified"))
        verify(exactly = 0) { airdropRepository.saveAirdropToken(any(), any()) }

        account.tapMainButton()
        verify(exactly = 1) { airdropRepository.saveAirdropToken("token", "refresh") }
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
        coVerify(exactly = 0) { airdropRepository.getAccountName("attacker") }
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
        coVerify(exactly = 1) { airdropRepository.getAccountName("next") }
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
        coEvery { airdropRepository.getAccountName("old") } answers {
            now += AirdropLoginDeeplinkPolicy.PENDING_LOGIN_TTL_MS
            openLink("new", "new-refresh")
            "old account"
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

        coVerify(exactly = 0) { airdropRepository.getAccountName(any()) }
        assertSame(newWarning, shownDialog())
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
