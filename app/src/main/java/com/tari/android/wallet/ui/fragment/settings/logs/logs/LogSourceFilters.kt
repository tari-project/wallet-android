package com.tari.android.wallet.ui.fragment.settings.logs.logs

import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.logging.LoggerTags
import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.DebugLog

enum class LogSourceFilters(val title: Int, val isMatch: (log: DebugLog) -> Boolean) {
    FFI(R.string.debug_log_filter_ffi, { it.auroraDebugLog == null }),
    AuroraGeneral(R.string.debug_log_filter_aurora_general, { it.auroraDebugLog != null }),
    AuroraUI(R.string.debug_log_filter_aurora_ui, { it.auroraDebugLog?.source1?.equals(LoggerTags.UI.name) == true }),
    AuroraNavigation(R.string.debug_log_filter_aurora_navigation, { it.auroraDebugLog?.source1?.equals(LoggerTags.Navigation.name) == true }),
    AuroraConnection(R.string.debug_log_filter_aurora_connection, { it.auroraDebugLog?.source1?.equals(LoggerTags.Connection.name) == true }),
}