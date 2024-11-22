package com.tari.android.wallet.ui.screen.tx.details.gif

import com.tari.android.wallet.ui.common.giphy.repository.GifItem

data class GifState(
    val gifItem: GifItem?,
    val error: Exception?,
) {

    init {
        require(gifItem == null || error == null) { "Both gif and error can't be nonnull" }
    }

    constructor() : this(null, null)
    constructor(gifItem: GifItem) : this(gifItem, null)
    constructor(e: Exception) : this(null, e)

    val isProcessing get() = gifItem == null && error == null
    val isError get() = error != null
    val isSuccessful get() = gifItem != null
}