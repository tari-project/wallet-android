package com.tari.android.wallet.model.seedPhrase

enum class SeedWordsWordPushResult(val value: Int) {
    InvalidSeedWord(0),
    SuccessfulPush(1),
    SeedPhraseComplete(2),
    InvalidSeedPhrase(3);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}