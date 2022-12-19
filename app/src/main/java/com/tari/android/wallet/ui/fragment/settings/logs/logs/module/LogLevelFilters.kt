package com.tari.android.wallet.ui.fragment.settings.logs.logs.module

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.DebugLog

enum class LogLevelFilters(val title: Int, val isMatch: (log: DebugLog) -> Boolean) {
    Error(R.string.debug_log_filter_error, { it.level.equals("error", true) }),
    Warning(R.string.debug_log_filter_warning, { it.level.equals("warning", true) }),
    Info(R.string.debug_log_filter_info, { it.level.equals("info", true) }),
    Debug(R.string.debug_log_filter_debug, { it.level.equals("debug", true) }),
}