package com.tari.android.wallet.model.seedPhrase

import com.tari.android.wallet.ffi.FFISeedWords

class SeedPhrase {

    var ffiSeedWords: FFISeedWords? = null
        private set

    fun init(words: List<String>): SeedPhraseCreationResult {
        val ffiSeedWords = FFISeedWords()

        try {
            for (seedWord in words) {
                return when (ffiSeedWords.pushWord(seedWord)) {
                    SeedWordsWordPushResult.InvalidSeedWord -> SeedPhraseCreationResult.InvalidSeedWord
                    SeedWordsWordPushResult.SuccessfulPush -> continue
                    SeedWordsWordPushResult.SeedPhraseComplete -> {
                        this.ffiSeedWords = ffiSeedWords
                        SeedPhraseCreationResult.Success
                    }

                    SeedWordsWordPushResult.InvalidSeedPhrase -> SeedPhraseCreationResult.InvalidSeedPhrase
                }
            }
        } catch (e: Throwable) {
            return SeedPhraseCreationResult.Failed(e)
        }

        return SeedPhraseCreationResult.SeedPhraseNotCompleted
    }


    sealed class SeedPhraseCreationResult {
        data object Success : SeedPhraseCreationResult()
        data class Failed(val exception: Throwable) : SeedPhraseCreationResult()
        data object InvalidSeedPhrase : SeedPhraseCreationResult()
        data object SeedPhraseNotCompleted : SeedPhraseCreationResult()
        data object InvalidSeedWord : SeedPhraseCreationResult()
    }

    companion object {
        const val SEED_PHRASE_LENGTH: Int = 24
    }
}