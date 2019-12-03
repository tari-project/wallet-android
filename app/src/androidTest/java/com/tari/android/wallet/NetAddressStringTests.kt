package com.tari.android.wallet

import android.util.Log
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.NetAddressString
import org.junit.Assert
import org.junit.Test
import java.util.*



class NetAddressStringTests {

    private val str = "0.0.0.0"
    private val str2 = "255.255.265.0"
    private val port = 0
    private val port2 = -10

    @Test
    fun testHexString() {
        val netaddress = NetAddressString(str,port)
        Assert.assertTrue(netaddress.toString().length > 4)
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun testHexStringArg1Exception() {
        val netAddressString = NetAddressString(str2,port)
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun testHexStringArg2Exception() {
        val netAddressString = NetAddressString(str,port2)
    }
}