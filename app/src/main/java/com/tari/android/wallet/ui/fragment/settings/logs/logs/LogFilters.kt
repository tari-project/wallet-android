package com.tari.android.wallet.ui.fragment.settings.logs.logs

import com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter.DebugLog

enum class LogFilters(val isMatch: (log: DebugLog) -> Boolean) {
    Info({ it.level.equals("info", true) }),
    Warning({ it.level.equals("warning", true) }),
    Error({ it.level.equals("error", true) }),
}