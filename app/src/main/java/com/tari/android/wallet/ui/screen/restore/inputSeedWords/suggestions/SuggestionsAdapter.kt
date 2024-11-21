package com.tari.android.wallet.ui.screen.restore.inputSeedWords.suggestions

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SuggestionsAdapter : CommonAdapter<SuggestionViewHolderItem>()  {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(SuggestionViewHolder.getBuilder())
}


