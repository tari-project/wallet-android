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
package com.tari.android.wallet.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build
import androidx.annotation.RequiresApi
import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.EventBus

/**
 * Receives network connection changes and posts changes to the event bus.
 *
 * @author The Tari Development Team
 */
internal class NetworkConnectionStateReceiver : BroadcastReceiver() {

    private val action = "android.net.conn.CONNECTIVITY_CHANGE"
    val intentFilter = IntentFilter(action)

    init {
        EventBus.networkConnectionState.post(NetworkConnectionState.UNKNOWN)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != action) {
            return
        }
        val mContext = context ?: return
        if (isInternetAvailable(mContext)) {
            Logger.d("Connected to the internet.")
            EventBus.networkConnectionState.post(NetworkConnectionState.CONNECTED)
        } else {
            Logger.d("Disconnected from the internet.")
            EventBus.networkConnectionState.post(NetworkConnectionState.DISCONNECTED)
        }
    }

    private fun isInternetAvailable(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        isConnectedNewApi(context)
    } else {
        isConnectedOld(context)
    }

    @Suppress("DEPRECATION")
    fun isConnectedOld(context: Context): Boolean {
        val connManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.activeNetworkInfo
        return networkInfo?.isConnected == true

    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isConnectedNewApi(context: Context): Boolean {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(NET_CAPABILITY_INTERNET) == true
    }
}