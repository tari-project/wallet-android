package com.tari.android.wallet.application.deeplinks

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class DeeplinkViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodesManager: BaseNodesManager

    @Inject
    lateinit var contactRepository: ContactsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var torSharedRepository: TorPrefRepository

    init {
        component.inject(this)
    }

    fun tryToHandle(qrData: String, isQrData: Boolean = true) {
        deeplinkHandler.handle(qrData)?.let { execute(it, isQrData) }
    }

    fun execute(deeplink: DeepLink, isQrData: Boolean = true) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> addBaseNode(deeplink, isQrData)
            is DeepLink.Contacts -> addContacts(deeplink, isQrData)
            is DeepLink.Send -> sendAction(deeplink, isQrData)
            is DeepLink.UserProfile -> addUserProfile(deeplink, isQrData)
            is DeepLink.TorBridges -> addTorBridges(deeplink, isQrData)
        }
    }

    private fun addBaseNode(deeplink: DeepLink.AddBaseNode, isQrData: Boolean = true) {
        val baseNode = getData(deeplink)
        val args = ConfirmDialogArgs(
            title = resourceManager.getString(R.string.home_custom_base_node_title),
            description = resourceManager.getString(R.string.home_custom_base_node_description),
            cancelButtonText = resourceManager.getString(R.string.home_custom_base_node_no_button),
            confirmButtonText = resourceManager.getString(R.string.common_lets_do_it),
            onConfirm = {
                hideDialog()
                addBaseNodeAction(baseNode, isQrData)
            },
        ).getModular(baseNode, resourceManager)
        showModularDialog(args)
    }

    private fun addUserProfile(deeplink: DeepLink.UserProfile, isQrData: Boolean) {
        val contact = DeepLink.Contacts(
            listOf(
                DeepLink.Contacts.DeeplinkContact(
                    alias = deeplink.alias,
                    tariAddress = deeplink.tariAddress,
                )
            )
        )
        addContacts(contact, isQrData)
    }

    fun addContacts(contacts: DeepLink.Contacts, isQrData: Boolean = true) {
        val contactDtos = getData(contacts)
        if (contactDtos.isEmpty()) return
        val names = contactDtos.joinToString(", ") { it.contactInfo.getAlias().trim() }
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.contact_deeplink_title)),
            BodyModule(resourceManager.getString(R.string.contact_deeplink_message, contactDtos.size.toString()) + ". " + names),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                addContactsAction(contactDtos, isQrData)
                hideDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
    }

    fun addTorBridges(deeplink: DeepLink.TorBridges, isQrData: Boolean) {
        deeplink.torConfigurations.forEach {
            torSharedRepository.addTorBridgeConfiguration(it)
        }
    }

    fun executeRawDeeplink(deeplink: DeepLink, isQrData: Boolean = true) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> addBaseNode(deeplink)
            is DeepLink.Contacts -> addContactsAction(getData(deeplink), isQrData)
            is DeepLink.Send -> sendAction(deeplink, isQrData)
            is DeepLink.UserProfile -> addContactsAction(getData(deeplink)?.let { listOf(it) } ?: listOf(), isQrData)
            is DeepLink.TorBridges -> addTorBridges(deeplink, isQrData)
        }
    }

    private fun getData(deeplink: DeepLink.AddBaseNode): BaseNodeDto = BaseNodeDto.fromDeeplink(deeplink)

    private fun getData(deeplink: DeepLink.Contacts): List<ContactDto> = deeplink.contacts.mapNotNull {
        runCatching {
            val tariWalletAddress = TariWalletAddress.fromBase58(it.tariAddress)
            ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = it.alias))
        }.getOrNull()
    }

    private fun getData(deeplink: DeepLink.Send): ContactDto? = runCatching {
        val tariWalletAddress = TariWalletAddress.fromBase58(deeplink.walletAddress)
        ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = ""))
    }.getOrNull()

    private fun getData(userProfile: DeepLink.UserProfile): ContactDto? = runCatching {
        val tariWalletAddress = TariWalletAddress.fromBase58(userProfile.tariAddress)
        ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = userProfile.alias))
    }.getOrNull()

    private fun addContactsAction(contacts: List<ContactDto>, isQrData: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.addContactList(contacts)
        }
    }

    private fun sendAction(deeplink: DeepLink.Send, isQrData: Boolean) {
        navigation.postValue(Navigation.TxListNavigation.ToSendWithDeeplink(deeplink))
    }

    private fun addBaseNodeAction(baseNodeDto: BaseNodeDto, isQrData: Boolean) {
        baseNodesManager.addUserBaseNode(baseNodeDto)
        baseNodesManager.setBaseNode(baseNodeDto)
        walletManager.syncBaseNode()
    }
}