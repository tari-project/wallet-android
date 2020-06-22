package com.tari.android.wallet

import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.NetAddressString
import org.junit.Assert.assertTrue
import org.junit.Test

class NetAddressStringTests {

    @Test
    fun constructor_assertThatValidInstanceWasCreated_ifValidAddressAndPortWereGiven() {
        assertTrue(NetAddressString("0.0.0.0", 0).toString().isNotEmpty())
    }

    @Test(expected = FFIException::class)
    fun constructor_assertThatFFIExceptionWasThrown_ifInvalidNetAddressWasGiven() {
        NetAddressString("255.255.265.0", 0)
    }

    @Test(expected = FFIException::class)
    fun constructor_assertThatFFIExceptionWasThrown_ifNegativePortNumberWasGiven() {
        NetAddressString("0.0.0.0", -10)
    }
}
