package com.tari.android.wallet.tor

data class TorBootstrapStatus(val progress: Int, val summary: String, val warning: String? = null) {

    companion object {

        private val WARNING_LOG_REGEX = Regex(".*PROGRESS=(\\d+).*SUMMARY=\"([^\"]*)\".*WARNING=\"([^\"]*)\".*")
        private val LOG_REGEX = Regex(".*PROGRESS=(\\d+).*SUMMARY=\"([^\"]*)\".*")
        const val MAX_PROGRESS = 100

        fun from(logLine: String): TorBootstrapStatus {
            if (WARNING_LOG_REGEX.matches(logLine)) {
                val matchResult = WARNING_LOG_REGEX.find(logLine)
                val (progress, summary, warning) = matchResult!!.destructured
                return TorBootstrapStatus(progress.toInt(), summary, warning)
            } else if (LOG_REGEX.matches(logLine)) {
                val matchResult = LOG_REGEX.find(logLine)
                val (progress, summary) = matchResult!!.destructured
                return TorBootstrapStatus(progress.toInt(), summary)
            }
            return TorBootstrapStatus(0, "")
        }
    }

}