package com.tari.android.wallet

import com.tari.android.wallet.ffi.HexString
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*


class HexStringTests {

    private val str = FFITestUtil.PUBLIC_KEY_HEX_STRING
    private val str2 = "Invalid Hex String"

    @Test
    fun testHexString() {
        val hex = HexString(str)
        assertTrue(hex.toString() == FFITestUtil.PUBLIC_KEY_HEX_STRING)
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun testHexStringException() {
        val hexString = HexString(str2)
        hexString.toString()
    }
}