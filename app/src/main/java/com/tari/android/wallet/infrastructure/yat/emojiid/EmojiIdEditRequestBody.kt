package com.tari.android.wallet.infrastructure.yat.emojiid

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EmojiIdEditRequestBody(
    @Expose @SerializedName("delete") val delete: List<String>? = null,
    @Expose @SerializedName("insert") val insert: List<EmojiIdInsertDTO>? = null,
    @Expose @SerializedName("merkle_root") val merkleRoot: String? = null,
    @Expose @SerializedName("signature") val signature: String? = null,
) {
    init {
        require(delete != null || insert != null || merkleRoot != null || signature != null) {
            "All arguments can't be null"
        }
    }
}

data class EmojiIdInsertDTO(
    @Expose @SerializedName("data") val data: String,
    @Expose @SerializedName("tag") val tag: TagTypeDTO,
)
