package com.tari.android.wallet

import com.tari.android.wallet.ffi.FFITransportType
import com.tari.android.wallet.ffi.nullptr
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FFITransportTypeTest {

    @Test
    fun emptyConstructor_assertThatValidMemoryTransportWasCreated() {
        val transport = FFITransportType()
        assertNotEquals(nullptr, transport.pointer)
        assertTrue(transport.getAddress().isNotEmpty())
        transport.destroy()
    }

    @Test
    fun netAddressConstructor_assertThatValidTransportWasCreated() {
        val transport = FFITransportType(FFITestUtil.address)
        assertNotEquals(nullptr, transport.pointer)
        transport.destroy()
    }
}