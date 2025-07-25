package com.tari.android.wallet.application.addressPoisoning

import androidx.annotation.VisibleForTesting
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.sharedPrefs.addressPoisoning.AddressPoisoningPrefRepository
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject
import javax.inject.Singleton

private const val MIN_SAME_CHARS = 3
private const val USED_PREFIX_SUFFIX_CHARS = Constants.Wallet.EMOJI_FORMATTER_CHUNK_SIZE

@Singleton
class AddressPoisoningChecker @Inject constructor(
    private val addressPoisoningSharedRepository: AddressPoisoningPrefRepository,
    private val contactsRepository: ContactsRepository,
    private val txRepository: TxRepository,
) {

    fun doOnAddressPoisoned(walletAddress: TariWalletAddress?, action: (similarContactList: List<SimilarAddressDto>) -> Unit) {
        if (walletAddress == null) return

        val similarContactList = walletAddress.findSimilarContacts()

        if (walletAddress.isPoisoned(similarContactList)) {
            action(similarContactList)
        }
    }

    fun markAsTrusted(walletAddress: TariWalletAddress, markAsTrusted: Boolean) {
        if (markAsTrusted) {
            addressPoisoningSharedRepository.addTrustedContact(walletAddress)
        } else {
            addressPoisoningSharedRepository.removeTrustedContact(walletAddress)
        }
    }

    private fun TariWalletAddress.isPoisoned(similarContactList: List<SimilarAddressDto>): Boolean {
        return if (DebugConfig.mockEveryAddressPoisoned) {
            this !in addressPoisoningSharedRepository.getTrustedContactList()
        } else {
            this !in addressPoisoningSharedRepository.getTrustedContactList()
                    && similarContactList.size > 1 // because the current wallet address is always similar to itself
        }
    }

    private fun TariWalletAddress.findSimilarContacts(): List<SimilarAddressDto> {
        return if (DebugConfig.mockPoisonedAddresses) {
            MockDataStub.createSimilarAddressList()
        } else {
            val allTxs = txRepository.txs.value.allTxs

            return (contactsRepository.contactList.value
                    // add the current wallet address to the list because it may not exist if there were no interactions with it
                    + contactsRepository.findOrCreateContact(this))
                .filter { it.walletAddress.isSimilarTo(this) }
                .map { contact ->
                    SimilarAddressDto(
                        contact = contact,
                        numberOfTransaction = allTxs.filterByWalletAddress(contact.walletAddress).size,
                        lastTransactionTimestampMillis = allTxs.filterByWalletAddress(contact.walletAddress)
                            .maxOfOrNull { it.tx.timestamp }
                            ?.let { it.toLong() * 1000L },
                        trusted = addressPoisoningSharedRepository.getTrustedContactList().contains(contact.walletAddress),
                    )
                }.let { similarContacts ->
                    // put the current wallet address to the first place
                    listOf(similarContacts.first { it.contact.walletAddress == this }) +
                            similarContacts.filter { it.contact.walletAddress != this }
                }
        }
    }

    private fun List<TxDto>.filterByWalletAddress(walletAddress: TariWalletAddress): List<TxDto> {
        return this.filter { it.tx.tariContact.walletAddress == walletAddress }
    }
}

@VisibleForTesting
fun TariWalletAddress?.isSimilarTo(other: TariWalletAddress): Boolean {
    if (this == null) return false
    if (this.paymentIdAddress && other.paymentIdAddress) return this.fullBase58 == other.fullBase58 // We need to compare full base58 for Tari addresses with payment IDs

    val thisEmojis = this.coreKeyEmojis.extractEmojis()
    val otherEmojis = other.coreKeyEmojis.extractEmojis()

    if (thisEmojis.size != otherEmojis.size || thisEmojis.size < (USED_PREFIX_SUFFIX_CHARS * 2)) {
        return false
    }

    val lShortText = thisEmojis.take(USED_PREFIX_SUFFIX_CHARS) + thisEmojis.takeLast(USED_PREFIX_SUFFIX_CHARS)
    val rShortText = otherEmojis.take(USED_PREFIX_SUFFIX_CHARS) + otherEmojis.takeLast(USED_PREFIX_SUFFIX_CHARS)

    var result = 0
    lShortText.zip(rShortText) { l, r ->
        if (l == r) {
            result++
        }
    }

    return result >= MIN_SAME_CHARS
}