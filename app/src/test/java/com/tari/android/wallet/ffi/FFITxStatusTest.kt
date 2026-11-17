package com.tari.android.wallet.ffi

import com.tari.android.wallet.model.TxStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FFITxStatusTest {

    @Test
    fun `maps every status value of LegacyTransactionStatus`() {
        val expected = listOf(
            -1 to FFITxStatus.TX_NULL_ERROR,
            0 to FFITxStatus.COMPLETED,
            1 to FFITxStatus.BROADCAST,
            2 to FFITxStatus.MINED_UNCONFIRMED,
            3 to FFITxStatus.IMPORTED,
            4 to FFITxStatus.PENDING,
            5 to FFITxStatus.COINBASE,
            6 to FFITxStatus.MINED_CONFIRMED,
            7 to FFITxStatus.REJECTED,
            8 to FFITxStatus.ONE_SIDED_UNCONFIRMED,
            9 to FFITxStatus.ONE_SIDED_CONFIRMED,
            10 to FFITxStatus.QUEUED,
            11 to FFITxStatus.COINBASE_UNCONFIRMED,
            12 to FFITxStatus.COINBASE_CONFIRMED,
            13 to FFITxStatus.COINBASE_NOT_IN_BLOCKCHAIN,
            14 to FFITxStatus.MINED_CONFIRMED_LOCKED,
            15 to FFITxStatus.ONE_SIDED_CONFIRMED_LOCKED,
            16 to FFITxStatus.COINBASE_CONFIRMED_LOCKED,
        )

        expected.forEach { (value, status) -> assertEquals("status $value", status, FFITxStatus.map(value)) }
    }

    @Test
    fun `maps a status value it does not know to UNKNOWN instead of throwing`() {
        listOf(17, 255, Int.MAX_VALUE, -2, Int.MIN_VALUE).forEach { value ->
            assertEquals("status $value", FFITxStatus.UNKNOWN, FFITxStatus.map(value))
        }
    }

    @Test
    fun `every FFI status has a model status`() {
        FFITxStatus.entries.forEach { ffiStatus -> assertEquals(ffiStatus.name, TxStatus.map(ffiStatus).name) }
    }

    @Test
    fun `locked statuses keep the one sided and coinbase classification`() {
        assertTrue(TxStatus.map(FFITxStatus.map(15)).isOneSided())
        assertFalse(TxStatus.map(FFITxStatus.map(15)).isCoinbase())
        assertTrue(TxStatus.map(FFITxStatus.map(16)).isCoinbase())
        assertFalse(TxStatus.map(FFITxStatus.map(16)).isOneSided())
        assertFalse(TxStatus.map(FFITxStatus.map(14)).isOneSided())
        assertFalse(TxStatus.map(FFITxStatus.map(14)).isCoinbase())
    }
}
