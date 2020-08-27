package com.tari.android.wallet.infrastructure.yat.emojiid

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EmojiIdSearchResponseBody(
    @Expose @SerializedName("status") val status: Boolean?,
    @Expose @SerializedName("result") val result: List<SearchResultEntryDTO>?,
    @Expose @SerializedName("error") val error: String?,
)

data class SearchResultEntryDTO(
    @Expose @SerializedName("tag") val tag: TagTypeDTO,
    @Expose @SerializedName("data") val data: String,
    @Expose @SerializedName("hash") val hash: String,
)
