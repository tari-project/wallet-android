/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.di

import android.content.Context
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFITransportType
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.nullptr
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.util.SharedPrefsWrapper
import dagger.Module
import dagger.Provides
import java.io.File
import java.net.ServerSocket
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject Tor-related dependencies.
 *
 * @author The Tari Development Team
 */
@Module
class TorModule {

    object FieldName {
        const val torProxyPort = "tor_proxy_port"
        const val torConnectionPort = "tor_connection_port"
        const val torControlPort = "tor_control_port"
        const val torControlHost = "tor_control_host"
        const val torCookieFilePath = "tor_cookie_file_path"
        const val torIdentity = "tor_identity"
        const val torSock5Username = "tor_sock5_username"
        const val torSock5Password = "tor_sock5_password"
    }

    /**
     * Provides a port for TOR connection
     */
    @Provides
    @Named(FieldName.torConnectionPort)
    @Singleton
    internal fun provideConnectionPort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    /**
     * Provides a port for TOR proxy
     */
    @Provides
    @Named(FieldName.torProxyPort)
    @Singleton
    internal fun provideTorProxyPort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    /**
     * Provides a port for TOR control
     */
    @Provides
    @Named(FieldName.torControlPort)
    @Singleton
    internal fun provideTorControlPort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    /**
     * Provides host for TOR control
     */
    @Provides
    @Named(FieldName.torControlHost)
    @Singleton
    internal fun provideTorControlAddress(): String {
        return "127.0.0.1"
    }

    /**
     * Provides cookie file path for TOR
     */
    @Provides
    @Named(FieldName.torCookieFilePath)
    @Singleton
    internal fun provideTorControlPassword(context: Context): String {
        return File(
            context.getDir(TorProxyManager.torDataDirectoryName, Context.MODE_PRIVATE),
            "control_auth_cookie"
        ).absolutePath
    }

    /**
     * Provides cookie file path for TOR
     */
    @Provides
    @Named(FieldName.torIdentity)
    @Singleton
    internal fun provideTorIdentity(): ByteArray {
        return ByteArray(0)
    }

    /**
     * Provides sock5 username for TOR
     */
    @Provides
    @Named(FieldName.torSock5Username)
    @Singleton
    internal fun provideTorSock5Username(): String {
        return "user123"
    }

    /**
     * Provides sock5 password for TOR
     */
    @Provides
    @Named(FieldName.torSock5Password)
    @Singleton
    internal fun provideTorSock5Password(): String {
        return "123456"
    }

    /**
     * Provides config for TOR
     */
    @Provides
    @Singleton
    internal fun provideTorConfig(
        @Named(FieldName.torControlHost) controlHost: String,
        @Named(FieldName.torControlPort) controlPort: Int,
        @Named(FieldName.torProxyPort) proxyPort: Int,
        @Named(FieldName.torConnectionPort) connectionPort: Int,
        @Named(FieldName.torCookieFilePath) cookieFilePath: String,
        @Named(FieldName.torIdentity) torIdentity: ByteArray,
        @Named(FieldName.torSock5Username) sock5Username: String,
        @Named(FieldName.torSock5Password) sock5Passsword: String
    ): TorConfig {
        return TorConfig(
            controlPort = controlPort,
            controlHost = controlHost,
            proxyPort = proxyPort,
            connectionPort = connectionPort,
            cookieFilePath = cookieFilePath,
            identity = torIdentity,
            sock5Username = sock5Username,
            sock5Password = sock5Passsword
        )
    }

    @Provides
    @Singleton
    internal fun provideTorProxyManager(
        context: Context,
        sharedPrefsWrapper: SharedPrefsWrapper,
        torConfig: TorConfig
    ): TorProxyManager {
        return TorProxyManager(
            context,
            sharedPrefsWrapper,
            torConfig
        )
    }

    /**
     * Provides transport for wallet to use
     */
    @Provides
    @Singleton
    internal fun provideTorTransport(torConfig: TorConfig): FFITransportType {
        // provide Tor transport
        val cookieFile = File(torConfig.cookieFilePath)
        var cookieString = ByteArray(0)
        if (cookieFile.exists()) {
            cookieString = cookieFile.readBytes()
        }

        val torCookie = FFIByteVector(cookieString)
        var torIdentity = FFIByteVector(nullptr)
        if (torConfig.identity.isNotEmpty()) {
            torIdentity.destroy()
            torIdentity = FFIByteVector(torConfig.identity)
        }
        return FFITransportType(
            NetAddressString(
                torConfig.controlHost,
                torConfig.controlPort
            ),
            torConfig.connectionPort,
            torCookie,
            torIdentity,
            torConfig.sock5Username,
            torConfig.sock5Password
        )
    }

}