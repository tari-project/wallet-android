package com.tari.android.wallet.service.seedPhrase

import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedPhraseRepository @Inject constructor() {

    private var seedPhrase: SeedPhrase? = null

    fun save(seedPhrase: SeedPhrase) {
        this.seedPhrase = seedPhrase
    }

    fun getPhrase(): SeedPhrase? = seedPhrase
}