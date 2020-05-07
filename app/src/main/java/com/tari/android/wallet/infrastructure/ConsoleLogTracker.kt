package com.tari.android.wallet.infrastructure

import android.content.Context
import android.util.Log

class ConsoleLogTracker : Tracker {
    override fun screen(path: String, title: String) {
        Log.i(TRACK_TAG, "Screen track issued with path \"$path\" and title \"$title\"")
    }

    override fun download(context: Context) {
        Log.i(TRACK_TAG, "Download track issued with $context")
    }

    override fun event(category: String, action: String) {
        Log.i(TRACK_TAG, "Event track issued with categoty \"$category\" and action \"$action\"")
    }

    private companion object {
        private const val TRACK_TAG = "Debug_Tracker"
    }
}
