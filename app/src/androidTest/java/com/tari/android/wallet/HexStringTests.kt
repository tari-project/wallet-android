package com.tari.android.wallet

import android.util.Log
import com.tari.android.wallet.ffi.HexString
import org.junit.Assert.*
import org.junit.Test
import java.util.*



class HexStringTests {

    private val str = TestUtil.PUBLIC_KEY_HEX_STRING
    private val str2 = "Invalid Hex String"

    @Test
    fun testHexString() {
        val hex = HexString(str)
        assertTrue(hex.toString() == TestUtil.PUBLIC_KEY_HEX_STRING)
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun testHexStringException() {
        var hexString = HexString(str2)
    }
}