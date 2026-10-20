package com.tari.android.wallet.ui.common

import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DialogManagerTest {

    private val dialogManager = DialogManager()

    private fun dialog(dialogId: Int, destroyed: Boolean = false) = mockk<ModularDialog>(relaxed = true) {
        every { args } returns ModularDialogArgs(dialogId = dialogId)
        every { isActivityDestroyed() } returns destroyed
    }

    @Test
    fun `dialog of a destroyed activity does not take the args of a new dialog`() {
        val dead = dialog(dialogId = 609)
        dialogManager.replace(dead)
        every { dead.isActivityDestroyed() } returns true

        val fresh = dialog(dialogId = 609)
        dialogManager.replace(fresh)

        verify(exactly = 0) { dead.applyArgs(any()) }
        verify(exactly = 1) { fresh.show() }
    }

    @Test
    fun `dialog of a finishing but not destroyed activity is still dismissed by id`() {
        val finishing = dialog(dialogId = 609)
        val other = dialog(dialogId = 700)
        dialogManager.replace(finishing)
        dialogManager.replace(other)

        dialogManager.dismiss(609)

        verify(exactly = 1) { finishing.dismiss(any()) }
        verify(exactly = 0) { other.dismiss(any()) }
    }
}
