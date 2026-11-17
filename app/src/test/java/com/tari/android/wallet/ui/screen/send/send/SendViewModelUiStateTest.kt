package com.tari.android.wallet.ui.screen.send.send

import com.tari.android.wallet.model.MicroTari
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

class SendViewModelUiStateTest {

    private fun state(feePerGram: MicroTari?) = SendViewModel.UiState(
        ticker = "XTM",
        contact = mockk(relaxed = true),
        amountValue = "3",
        fee = MicroTari(BigInteger.valueOf(229)),
        feePerGram = feePerGram,
        availableBalance = MicroTari(BigInteger.valueOf(10_000_000)),
    )

    @Test
    fun `transaction data carries the fee rate the shown fee was estimated with`() {
        val transactionData = state(feePerGram = MicroTari(BigInteger.valueOf(7))).transactionData

        assertNotNull(transactionData)
        assertEquals(MicroTari(BigInteger.valueOf(229)), transactionData?.fee)
        assertEquals(MicroTari(BigInteger.valueOf(7)), transactionData?.feePerGram)
    }

    @Test
    fun `transaction data has no fee rate when none was stored`() {
        val transactionData = state(feePerGram = null).transactionData

        assertNotNull(transactionData)
        assertNull(transactionData?.feePerGram)
    }
}
