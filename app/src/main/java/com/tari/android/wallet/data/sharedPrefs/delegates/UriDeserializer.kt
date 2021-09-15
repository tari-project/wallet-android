package com.tari.android.wallet.data.sharedPrefs.delegates

import android.net.Uri
import com.google.gson.*
import java.lang.reflect.Type

class UriDeserializer : JsonDeserializer<Uri>, JsonSerializer<Uri> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Uri = Uri.parse(json?.asString)

    override fun serialize(src: Uri?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement = JsonPrimitive(src.toString())
}