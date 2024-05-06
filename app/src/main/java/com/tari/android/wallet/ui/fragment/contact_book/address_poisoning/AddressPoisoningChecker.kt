package com.tari.android.wallet.ui.fragment.contact_book.address_poisoning

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressPoisoningChecker @Inject constructor(

) {
    fun isPoisoned(walletAddress: TariWalletAddress): Boolean {
        return if (DebugConfig.mockAddressPoisoning) {
            true
        } else {
            false // todo implement
        }
    }

    fun getSimilarContactList(walletAddress: TariWalletAddress): List<SimilarAddressItem> {
        return if (DebugConfig.mockAddressPoisoning) {
            MockDataStub.createSimilarAddressList()
        } else {
            listOf()// todo implement
        }
    }
}