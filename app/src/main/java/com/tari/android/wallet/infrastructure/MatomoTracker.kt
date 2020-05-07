package com.tari.android.wallet.infrastructure

import android.content.Context
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.TrackHelper
import org.matomo.sdk.Tracker as MatomoSdkTracker

class MatomoTracker(private val tracker: MatomoSdkTracker) : Tracker {
    override fun screen(path: String, title: String) = TrackHelper.track()
        .screen(path)
        .title(title)
        .with(this.tracker)

    override fun download(context: Context) = TrackHelper.track()
        .download()
        .identifier(DownloadTracker.Extra.ApkChecksum(context))
        .with(this.tracker)

    override fun event(category: String, action: String) = TrackHelper.track()
        .event(category, action)
        .with(this.tracker)
}
