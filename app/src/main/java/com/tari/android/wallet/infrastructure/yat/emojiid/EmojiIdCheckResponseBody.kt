package com.tari.android.wallet.infrastructure.yat.emojiid

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EmojiIdCheckResponseBody(
    @Expose @SerializedName("result") val result: SearchResultDTO,
)

data class SearchResultDTO(
    @Expose @SerializedName("emoji_id") val emojiId: String,
    @Expose @SerializedName("available") val isAvailable: Boolean,
    @Expose @SerializedName("price") val price: Int,
    @Expose @SerializedName("discounted_price") val discountedPrice: Int,
)
