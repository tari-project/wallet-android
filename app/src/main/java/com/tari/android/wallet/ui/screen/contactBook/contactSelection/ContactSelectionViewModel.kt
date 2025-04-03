package com.tari.android.wallet.ui.screen.contactBook.contactSelection

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.chat.ChatItemDto
import com.tari.android.wallet.data.chat.ChatsRepository
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.data.contacts.model.FFIContactInfo
import com.tari.android.wallet.data.contacts.model.splitAlias
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.infrastructure.ShareManager
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.component.clipboardController.WalletAddressViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.AddressPoisoningModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.screen.contactBook.addressPoisoning.AddressPoisoningChecker
import com.tari.android.wallet.ui.screen.contactBook.addressPoisoning.SimilarAddressDto
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionModel.Effect
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionModel.YatState
import com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.contact.ContactItemViewHolderItem
import com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.contact.ContactlessPaymentItem
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.EmojiUtil.Companion.getGraphemeLength
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class ContactSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var chatsRepository: ChatsRepository

    @Inject
    lateinit var addressPoisoningChecker: AddressPoisoningChecker

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    var additionalFilter: (ContactItemViewHolderItem) -> Boolean = { true }

    val selectedContact = MutableLiveData<ContactDto>()

    val selectedTariWalletAddress = MutableLiveData<TariWalletAddress?>()

    private val contactListSource = MediatorLiveData<List<ContactItemViewHolderItem>>()

    private val searchText = MutableLiveData("")

    val contactList = MediatorLiveData<List<CommonViewHolderItem>>()

    val clipboardChecker = MediatorLiveData<Unit>()

    val walletAddressViewModel = WalletAddressViewModel()

    val isContactlessPayment = MutableLiveData(false)

    val amount: MutableLiveData<MicroTari> = MutableLiveData()

    private val _effect = EffectFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    private val _yatState = MutableStateFlow(YatState())
    val yatState = _yatState.asStateFlow()

    init {
        component.inject(this)

        doOnWalletRunning {
            walletAddressViewModel.checkClipboardForValidEmojiId()
        }

        collectFlow(contactsRepository.contactList) {
            contactListSource.value = it.map { contactDto -> ContactItemViewHolderItem(contact = contactDto, isSimple = true) }
        }
        contactList.addSource(contactListSource) { updateContactList() }
        contactList.addSource(searchText) { updateContactList() }
        contactList.addSource(isContactlessPayment) { updateContactList() }
    }

    override fun handleDeeplink(deeplink: DeepLink) {
        if (deeplink is DeepLink.Send || deeplink is DeepLink.Contacts || deeplink is DeepLink.UserProfile) {
            val deeplinkBase58 = when (deeplink) {
                is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.tariAddress
                is DeepLink.Send -> deeplink.walletAddress
                is DeepLink.UserProfile -> deeplink.tariAddress
                else -> null
            }

            val name = when (deeplink) {
                is DeepLink.Contacts -> deeplink.contacts.firstOrNull()?.alias
                is DeepLink.UserProfile -> deeplink.alias
                else -> null
            }.orEmpty()

            when (deeplink) {
                is DeepLink.Send -> deeplink.amount?.let { amount.value = it }
                else -> Unit
            }

            deeplinkBase58?.let { TariWalletAddress.fromBase58OrNull(it) }?.let { walletAddress ->
                selectedContact.value = ContactDto(FFIContactInfo(walletAddress), uuid = name)
                _yatState.update { it.copy(yatUser = null) }
            } ?: run { logger.e("Wallet address not found for deeplink: $deeplink") }
        } else {
            super.handleDeeplink(deeplink)
        }
    }

    fun onContactlessPaymentClick() {
        ShareManager.currentInstant?.doContactlessPayment()
    }

    fun onContactClick(contact: ContactDto) {
        selectedContact.value = contact
        _yatState.update { it.copy(yatUser = null) }
    }

    fun deselectTariWalletAddress() {
        selectedTariWalletAddress.value = null
    }

    fun toggleYatEye() {
        _yatState.update { it.toggleEye() }
    }

    fun addressEntered(addressText: EmojiId) {
        launchOnIo {
            val yatUser = tryToFindYatUser(addressText)
            _yatState.update { it.copy(yatUser = yatUser) }
            val walletAddress = TariWalletAddress.fromEmojiIdOrNull(addressText)
            selectedTariWalletAddress.postValue(walletAddress)

            addressPoisoningChecker.doOnAddressPoisoned(walletAddress) { addresses ->
                showAddressPoisonedDialog(addresses)
            }

            if (yatUser == null && walletAddress == null) {
                searchText.postValue(addressText)
                if (addressText.isNotEmpty()) {
                    _effect.send(Effect.ShowNotValidEmojiId)
                }
            } else {
                _effect.send(Effect.ShowNextButton)
            }
        }
    }

    fun onContinueButtonClick(effect: ContinueButtonEffect) {
        when (effect) {
            is ContinueButtonEffect.AddContact -> {
                val user = getUserDto()
                val fullName = effect.name
                val firstName = splitAlias(fullName).firstName
                val lastName = splitAlias(fullName).lastName

                if (user.walletAddress == corePrefRepository.walletAddress) {
                    showCantAddYourselfDialog()
                } else {
                    launchOnIo {
                        contactsRepository.updateContactInfo(user, firstName, lastName, "")
                        launchOnMain {
                            tariNavigator.navigate(Navigation.ContactBook.BackToContactBook)
                        }
                    }
                }
            }

            is ContinueButtonEffect.SelectUserContact -> {
                val user = getUserDto()
                if (user.walletAddress == corePrefRepository.walletAddress) {
                    showCantSendYourselfDialog()
                } else {
                    tariNavigator.navigate(Navigation.TxList.ToSendTariToUser(user, amount.value))
                }
            }

            is ContinueButtonEffect.AddChat -> {
                val user = getUserDto()
                val chatDto = ChatItemDto(UUID.randomUUID().toString(), listOf(), user.getFFIContactInfo()!!.walletAddress)
                chatsRepository.addChat(chatDto)

                tariNavigator.navigate(Navigation.Chat.ToChat(user.getFFIContactInfo()?.walletAddress!!, true))
            }
        }
    }

    fun parseDeeplink(deeplinkString: String) {
        val deeplink = deeplinkManager.parseDeepLink(deeplinkString)!!
        deeplinkManager.execute(this, deeplink)
        deselectTariWalletAddress()
    }

    private fun showCantAddYourselfDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.contact_book_add_contact_cant_add_yourself_title),
            description = resourceManager.getString(R.string.contact_book_add_contact_cant_add_yourself_description),
        )
    }

    private fun showCantSendYourselfDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.contact_book_select_contact_cant_send_to_yourself_title),
            description = resourceManager.getString(R.string.contact_book_select_contact_cant_send_to_yourself_description),
        )
    }

    private fun getUserDto(): ContactDto =
        yatState.value.yatUser?.let { ContactDto(contactInfo = FFIContactInfo(it.walletAddress)) }
            ?: selectedContact.value
            ?: contactListSource.value.orEmpty()
                .firstOrNull { it.contact.contactInfo.extractWalletAddress() == selectedTariWalletAddress.value }?.contact
            ?: ContactDto(FFIContactInfo(selectedTariWalletAddress.value!!))

    private fun updateContactList() {
        val source = contactListSource.value ?: return
        val searchText = searchText.value ?: return

        var list = source.filter { additionalFilter.invoke(it) }

        if (searchText.isNotEmpty()) {
            list = list.filter { it.filtered(searchText) }
        }

        val resentUsed = list.filter { it.contact.getFFIContactInfo()?.lastUsedTimeMillis != null }
            .sortedByDescending { it.contact.getFFIContactInfo()?.lastUsedTimeMillis }
            .take(Constants.Contacts.RECENT_CONTACTS_COUNT)

        val restOfContact = list.filter { !resentUsed.contains(it) }.sortedBy { it.contact.contactInfo.getAlias().lowercase() }

        contactList.postValue(
            listOfNotNull(
                ContactlessPaymentItem().takeIf { isContactlessPayment.value == true },

                TitleViewHolderItem(resourceManager.getString(R.string.add_recipient_recent_tx_contacts)).takeIf { resentUsed.isNotEmpty() },
                *resentUsed.toTypedArray(),

                TitleViewHolderItem(resourceManager.getString(R.string.add_recipient_my_contacts))
                    .takeIf { restOfContact.isNotEmpty() && resentUsed.isNotEmpty() },
                *restOfContact.toTypedArray(),
            )
        )
    }

    private suspend fun tryToFindYatUser(yatEmojiId: EmojiId): YatState.YatUser? {
        if (yatEmojiId.isEmpty() || yatEmojiId.getGraphemeLength() > YAT_MAX_LENGTH) return null

        return yatAdapter.searchTariYat(yatEmojiId)?.let { yatResult ->
            YatState.YatUser(
                yat = yatEmojiId,
                walletAddress = TariWalletAddress.fromBase58(yatResult.address),
            )
        }
    }

    private fun showAddressPoisonedDialog(similarAddressList: List<SimilarAddressDto>) {
        val addressPoisoningModule = AddressPoisoningModule(
            addresses = similarAddressList,
        )
        val continueButtonModule = ButtonModule(
            text = resourceManager.getString(R.string.common_continue),
            style = ButtonStyle.Normal,
            action = {
                similarAddressDialogContinueClick(
                    selectedAddressItem = addressPoisoningModule.selectedAddress,
                    markAsTrusted = addressPoisoningModule.markAsTrusted,
                )
            }
        )
        val cancelButtonModule = ButtonModule(
            text = resourceManager.getString(R.string.common_cancel),
            style = ButtonStyle.Close,
        )

        showModularDialog(
            addressPoisoningModule,
            continueButtonModule,
            cancelButtonModule,
        )
    }

    private fun similarAddressDialogContinueClick(selectedAddressItem: SimilarAddressDto, markAsTrusted: Boolean) {
        selectedAddressItem.contactDto.contactInfo.requireWalletAddress().let { selectedAddress ->
            addressPoisoningChecker.markAsTrusted(selectedAddress, markAsTrusted)
            viewModelScope.launch(Dispatchers.Main) {
                hideDialog()
                _effect.send(Effect.GoToNext)
            }
        }
    }

    companion object {
        private const val YAT_MAX_LENGTH = 5
    }

    sealed class ContinueButtonEffect {
        data class AddContact(val name: String) : ContinueButtonEffect()
        data object SelectUserContact : ContinueButtonEffect()
        data object AddChat : ContinueButtonEffect()
    }
}
