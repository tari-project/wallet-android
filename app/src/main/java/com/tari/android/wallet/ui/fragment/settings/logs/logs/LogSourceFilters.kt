package com.tari.android.wallet.ui.fragment.settings.logs.logs

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.DebugLog

enum class LogSourceFilters(val title: Int, val isMatch: (log: DebugLog) -> Boolean) {
    FFI(R.string.debug_log_filter_ffi, { it.auroraDebugLog == null }),
    Aurora(R.string.debug_log_filter_aurora, { it.auroraDebugLog != null }),
}