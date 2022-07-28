package com.tari.android.wallet.ui.fragment.restore.inputSeedWords.suggestions

sealed class SuggestionState {
    object Hidden : SuggestionState()

    object NotStarted : SuggestionState()

    class Suggested(val list: MutableList<String>) : SuggestionState()

    object Empty : SuggestionState()
}