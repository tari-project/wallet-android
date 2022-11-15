package com.tari.android.wallet

import com.tari.android.wallet.ffi.HexString
import org.junit.Assert.assertEquals
import org.junit.Test

class HexStringTests {

    @Test
    fun toString_assertThatGivenConstructorArgumentWasReturned() {
        assertEquals(
            FFITestUtil.PUBLIC_KEY_HEX_STRING,
            HexString(FFITestUtil.PUBLIC_KEY_HEX_STRING).toString()
        )
    }
}
