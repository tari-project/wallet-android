package com.tari.android.wallet.model.seedPhrase

import com.orhanobut.logger.Logger
import com.tari.android.wallet.ffi.FFISeedWords

class SeedPhrase {

    sealed class SeedPhraseCreationResult {
        data class Success(val ffiSeedWords: FFISeedWords) : SeedPhraseCreationResult()
        data class Failed(val exception: Throwable) : SeedPhraseCreationResult()
        data object InvalidSeedPhrase : SeedPhraseCreationResult()
        data object SeedPhraseNotCompleted : SeedPhraseCreationResult()
        data object InvalidSeedWord : SeedPhraseCreationResult()
    }

    companion object {
        private val logger
            get() = Logger.t(SeedPhrase::class.simpleName)

        const val SEED_PHRASE_LENGTH: Int = 24

        fun create(words: List<String>): SeedPhraseCreationResult {
            val ffiSeedWords = FFISeedWords()

            try {
                for (seedWord in words) {
                    return when (ffiSeedWords.pushWord(seedWord)) {
                        SeedWordsWordPushResult.InvalidSeedWord -> SeedPhraseCreationResult.InvalidSeedWord
                        SeedWordsWordPushResult.SuccessfulPush -> continue
                        SeedWordsWordPushResult.SeedPhraseComplete -> SeedPhraseCreationResult.Success(ffiSeedWords)
                        SeedWordsWordPushResult.InvalidSeedPhrase -> SeedPhraseCreationResult.InvalidSeedPhrase
                    }
                }
            } catch (e: Throwable) {
                return SeedPhraseCreationResult.Failed(e)
            }

            return SeedPhraseCreationResult.SeedPhraseNotCompleted
        }

        fun createOrNull(words: List<String>?): FFISeedWords? {
            return words?.let {
                when (val result = create(words)) {
                    is SeedPhraseCreationResult.Success -> result.ffiSeedWords
                    else -> {
                        logger.i("Seed phrase creation failed: $result")
                        null
                    }
                }
            }
        }
    }
}