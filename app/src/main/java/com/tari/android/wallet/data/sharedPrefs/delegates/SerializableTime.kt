package com.tari.android.wallet.data.sharedPrefs.delegates

import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime
import java.io.Serializable

class SerializableTime(dateTime: DateTime) : Serializable {
    @SerializedName("timeInLong")
    val timeInLong: Long = dateTime.millis

    val date: DateTime
        get() = DateTime(timeInLong)
}