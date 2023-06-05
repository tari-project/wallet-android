package com.tari.android.wallet.ui.fragment.restore.inputSeedWords.suggestions

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class SuggestionViewHolderItem(val suggestion: String) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "SuggestionViewHolderItem$suggestion"
}