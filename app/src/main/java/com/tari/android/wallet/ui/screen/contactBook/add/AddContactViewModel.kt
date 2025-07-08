package com.tari.android.wallet.ui.screen.contactBook.add

import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AddContactViewModel() : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun saveContact() {
        uiState.value.walletAddress?.let { walletAddress ->
            if (walletAddress == corePrefRepository.walletAddress) {
                showCantAddYourselfDialog()
            } else {
                launchOnIo {
                    val newContact = Contact(
                        walletAddress = walletAddress,
                        alias = uiState.value.alias.trim(),
                    )
                    contactsRepository.addContact(newContact)

                    tariNavigator.navigateSequence(
                        Navigation.Back,
                        Navigation.ContactBook.ContactDetails(newContact),
                    )
                }
            }
        }
    }

    override fun handleDeeplink(deeplink: DeepLink) {
        deeplink.getTariAddressOrNull()?.let { onAddressChange(it) }
        deeplink.getAliasOrNull()?.let { onAliasChange(it) }
    }

    private fun showCantAddYourselfDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.contact_book_add_contact_cant_add_yourself_title),
            description = resourceManager.getString(R.string.contact_book_add_contact_cant_add_yourself_description),
        )
    }

    fun onAliasChange(alias: String) {
        _uiState.update { it.copy(alias = alias) }
    }

    fun onAddressChange(addressValue: String) {
        TariWalletAddress.makeTariAddressOrNull(addressValue).let { address ->
            _uiState.update {
                it.copy(
                    walletAddress = address,
                    isValidWalletAddress = addressValue.isBlank() || address != null,
                )
            }
        }
    }

    data class UiState(
        val alias: String = "",
        val walletAddress: TariWalletAddress? = null,
        val isValidWalletAddress: Boolean = true,
    ) {
        val saveButtonEnabled: Boolean
            get() = alias.isNotBlank() && walletAddress != null
    }
}