package com.tari.android.wallet.service.seedPhrase

import com.tari.android.wallet.model.seedPhrase.SeedPhrase

class SeedPhraseRepository {

    private var seedPhrase: SeedPhrase? = null

    fun save(seedPhrase: SeedPhrase) {
        this.seedPhrase = seedPhrase
    }

    fun getPhrase() : SeedPhrase? = seedPhrase
}