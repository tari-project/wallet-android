/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.util.extension

import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.domain.ResourceManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Contains Date functions.
 *
 * @author The Tari Development Team
 */

fun Date.txFormattedDate(): String {
    val cal: Calendar = Calendar.getInstance()
    cal.time = this
    val day: Int = cal.get(Calendar.DATE)
    var indicator = "th"
    if (day !in 11..18) indicator = when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return SimpleDateFormat("MMMM d'$indicator' yyyy 'at' h:mm a", Locale.ENGLISH)
        .format(this)
}

fun Calendar.isAfterNow(): Boolean {
    return this.after(Calendar.getInstance())
}

fun Date.formatToRelativeTime(resourceManager: ResourceManager): String {
    val now = Date().time
    val diff = now - time

    // Calculate time difference in seconds, minutes, hours, and days
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    // Format the relative time string
    return when {
        days > 2 -> DateFormat.getDateInstance().format(this)
        days > 0 -> resourceManager.getString(R.string.tx_list_days_ago, days)
        hours > 0 -> resourceManager.getString(R.string.tx_list_hours_ago, hours)
        minutes > 0 -> resourceManager.getString(R.string.tx_list_minutes_ago, minutes)
        seconds > 0 -> resourceManager.getString(R.string.tx_list_seconds_ago, minutes)
        else -> resourceManager.getString(R.string.tx_list_just_now)
    }
}

fun Date.minusHours(hours: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, -hours)
    return calendar.time
}