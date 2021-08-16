package com.tari.android.wallet

import androidx.test.core.app.ApplicationProvider
import com.tari.android.wallet.di.TorModule
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFITransportType
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.nullptr
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun torConstructor_assertThatValidTransportWasConstructed() {
        val torMod = TorModule()
        val cookieFile =
            File(torMod.provideTorCookieFilePath(ApplicationProvider.getApplicationContext()))
        val cookie =
            if (cookieFile.exists()) FFIByteVector(
                cookieFile.readBytes()
            )
            else FFIByteVector(nullptr)
        val addressString =
            NetAddressString(
                torMod.provideTorControlAddress(),
                torMod.provideTorControlPort()
            )
        val transport = FFITransportType(
            addressString,
            cookie,
            torMod.provideConnectionPort(),
            torMod.provideTorSock5Username(),
            torMod.provideTorSock5Password()
        )
        assertNotEquals(nullptr, transport.pointer)
        transport.destroy()
    }

}