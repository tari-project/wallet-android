package com.tari.android.wallet

import com.tari.android.wallet.ffi.HexString
import org.junit.Assert.assertEquals
import org.junit.Test

class HexStringTests {

    @Test
    fun toString_assertThatGivenConstructorArgumentWasReturned() {
        assertEquals(
            FFITestUtil.WALLET_ADDRESS_HEX_STRING,
            HexString(FFITestUtil.WALLET_ADDRESS_HEX_STRING).toString()
        )
    }
}
