package com.tari.android.wallet.infrastructure

import android.content.Context

interface Tracker {

    fun screen(path: String, title: String)

    fun download(context: Context)

    fun event(category: String, action: String)

}
