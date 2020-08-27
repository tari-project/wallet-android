package com.tari.android.wallet.infrastructure.yat.emojiid

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

enum class TagTypeDTO {
    @Expose
    @SerializedName("0x0101")
    TARI_PUBKEY,
}
