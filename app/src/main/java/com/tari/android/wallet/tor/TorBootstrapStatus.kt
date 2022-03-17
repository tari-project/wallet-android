package com.tari.android.wallet.tor

import com.orhanobut.logger.Logger

data class TorBootstrapStatus(val progress: Int, val summary: String, val warning: String? = null) {

    companion object {

        private val warningLogRegex = Regex(".*PROGRESS=(\\d+).*SUMMARY=\"([^\"]*)\".*WARNING=\"([^\"]*)\".*")
        private val logRegex = Regex(".*PROGRESS=(\\d+).*SUMMARY=\"([^\"]*)\".*")
        const val maxProgress = 100

        fun from(logLine: String): TorBootstrapStatus {
            Logger.i("tor connection status: $logLine")
            if (warningLogRegex.matches(logLine)) {
                val matchResult = warningLogRegex.find(logLine)
                val (progress, summary, warning) = matchResult!!.destructured
                return TorBootstrapStatus(progress.toInt(), summary, warning)
            } else if (logRegex.matches(logLine)) {
                val matchResult = logRegex.find(logLine)
                val (progress, summary) = matchResult!!.destructured
                return TorBootstrapStatus(progress.toInt(), summary)
            }
            return TorBootstrapStatus(0, "")
        }
    }

}