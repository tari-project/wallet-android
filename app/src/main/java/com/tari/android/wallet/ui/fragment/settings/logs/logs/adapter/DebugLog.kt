package com.tari.android.wallet.ui.fragment.settings.logs.logs.adapter

class DebugLog(var line: String) {

    var timestamp: String = ""
        private set

    var source1: String = ""
        private set

    var source2: String = ""
        private set

    var level: String = ""
        private set

    var log: String = ""
        private set

    var auroraDebugLog: DebugLog? = null
        private set

    init {
        if (ffiRegex.matches(line)) {
            val matchResult = ffiRegex.find(line)
            val (timestamp, source1, _, level, log) = matchResult!!.destructured
            this.timestamp = timestamp
            this.source1 = source1.replace("[", "").replace("]", "")
            this.source2 = source1.replace("[", "").replace("]", "")
            this.level = level
            this.log = log

            if (ffiRegex.matches(log)) {
                auroraDebugLog = DebugLog(log)
            }
        }
    }

    companion object {
        private val ffiRegex = Regex("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\w+)\\s(\\[[^]]+])\\s(\\[[^]]+]\\s)?([A-Z]+)\\s(.+)")
    }
}