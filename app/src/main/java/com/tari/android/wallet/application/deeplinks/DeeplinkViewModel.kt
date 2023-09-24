package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import javax.inject.Inject

class DeeplinkViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var baseNodeRepository: BaseNodeSharedRepository

    @Inject
    lateinit var contactRepository: ContactsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    @Inject
    lateinit var torSharedRepository: TorSharedRepository

    init {
        component.inject(this)
    }

    fun tryToHandle(qrData: String) {
        deeplinkHandler.handle(qrData)?.let { execute(it) }
    }

    fun execute(deeplink: DeepLink) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> addBaseNode(deeplink)
            is DeepLink.Contacts -> addContacts(deeplink)
            is DeepLink.Send -> sendAction(deeplink)
            is DeepLink.UserProfile -> addUserProfile(deeplink)
            is DeepLink.TorBridges -> addTorBridges(deeplink)
        }
    }

    fun addBaseNode(deeplink: DeepLink.AddBaseNode) {
        val baseNode = getData(deeplink)
        val args = ConfirmDialogArgs(
            resourceManager.getString(R.string.home_custom_base_node_title),
            resourceManager.getString(R.string.home_custom_base_node_description),
            resourceManager.getString(R.string.home_custom_base_node_no_button),
            resourceManager.getString(R.string.common_lets_do_it),
            onConfirm = {
                dismissDialog.postValue(Unit)
                addBaseNodeAction(baseNode)
            }
        ).getModular(baseNode, resourceManager)
        modularDialog.postValue(args)
    }

    fun addUserProfile(deeplink: DeepLink.UserProfile) {
        val contact = DeepLink.Contacts(
            listOf(
                DeepLink.Contacts.DeeplinkContact(
                    deeplink.alias,
                    deeplink.tariAddressHex
                )
            )
        )
        addContacts(contact)
    }

    fun addContacts(contacts: DeepLink.Contacts) {
        val contactDtos = getData(contacts)
        if (contactDtos.isEmpty()) return
        val names = contactDtos.joinToString(", ") { it.contact.getAlias().trim() }
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.contact_deeplink_title)),
                BodyModule(resourceManager.getString(R.string.contact_deeplink_message, contactDtos.size.toString()) + ". " + names),
                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                    addContactsAction(contactDtos)
                    dismissDialog.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    fun addTorBridges(deeplink: DeepLink.TorBridges) {
        deeplink.torConfigurations.forEach {
            torSharedRepository.addTorBridgeConfiguration(it)
        }
    }

    fun executeRawDeeplink(deeplink: DeepLink) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> addBaseNode(deeplink)
            is DeepLink.Contacts -> addContactsAction(getData(deeplink))
            is DeepLink.Send -> sendAction(deeplink)
            is DeepLink.UserProfile -> addContactsAction(getData(deeplink)?.let { listOf(it) } ?: listOf())
            is DeepLink.TorBridges -> addTorBridges(deeplink)
        }
    }

    private fun getData(deeplink: DeepLink.AddBaseNode): BaseNodeDto = BaseNodeDto.fromDeeplink(deeplink)

    private fun getData(deeplink: DeepLink.Contacts): List<ContactDto> = deeplink.contacts.mapNotNull {
        runCatching {
            val ffiWalletAddress = FFITariWalletAddress(HexString(it.hex))
            val tariWalletAddress = TariWalletAddress(ffiWalletAddress.toString(), ffiWalletAddress.getEmojiId())
            ContactDto(FFIContactDto(tariWalletAddress, it.alias))
        }.getOrNull()
    }

    private fun getData(deeplink: DeepLink.Send): ContactDto? = runCatching {
        val ffiWalletAddress = FFITariWalletAddress(HexString(deeplink.walletAddressHex))
        val tariWalletAddress = TariWalletAddress(ffiWalletAddress.toString(), ffiWalletAddress.getEmojiId())
        ContactDto(FFIContactDto(tariWalletAddress, ""))
    }.getOrNull()

    private fun getData(userProfile: DeepLink.UserProfile): ContactDto? = runCatching {
        val ffiWalletAddress = FFITariWalletAddress(HexString(userProfile.tariAddressHex))
        val tariWalletAddress = TariWalletAddress(ffiWalletAddress.toString(), ffiWalletAddress.getEmojiId())
        ContactDto(FFIContactDto(tariWalletAddress, userProfile.alias))
    }.getOrNull()

    private fun addContactsAction(contacts: List<ContactDto>) {
        _backPressed.postValue(Unit)
        contacts.forEach { contactRepository.addContact(it) }
    }

    private fun sendAction(deeplink: DeepLink.Send) {
        navigation.postValue(Navigation.TxListNavigation.ToSendWithDeeplink(deeplink))
    }

    private fun addBaseNodeAction(baseNodeDto: BaseNodeDto) {
        baseNodeRepository.addUserBaseNode(baseNodeDto)
        baseNodes.setBaseNode(baseNodeDto)
    }
}