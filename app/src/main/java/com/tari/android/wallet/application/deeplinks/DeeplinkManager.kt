package com.tari.android.wallet.application.deeplinks

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeeplinkManager @Inject constructor(
    private val baseNodesManager: BaseNodesManager,
    private val contactRepository: ContactsRepository,
    private val torSharedRepository: TorPrefRepository,
    private val resourceManager: ResourceManager,
    private val dialogManager: DialogManager,
    private val walletManager: WalletManager,
    private val navigator: TariNavigator,
    private val deeplinkParser: DeeplinkParser,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    fun parseDeepLink(deepLink: String): DeepLink? = deeplinkParser.parse(deepLink)

    fun getDeeplinkString(deeplink: DeepLink): String = deeplinkParser.toDeeplink(deeplink)

    /**
     * Executes the given deeplink, but first shows a confirmation dialog.
     */
    fun execute(context: Context, deeplink: DeepLink, isQrData: Boolean = true) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> showAddBaseNodeDialog(context, deeplink, isQrData)
            is DeepLink.Contacts -> showAddContactsDialog(context, deeplink, isQrData)
            is DeepLink.Send -> sendAction(deeplink, isQrData)
            is DeepLink.UserProfile -> addUserProfile(context, deeplink, isQrData)
            is DeepLink.TorBridges -> addTorBridges(deeplink, isQrData)
            is DeepLink.PaperWallet -> showPaperWalletDialog(context, deeplink, isQrData)
        }
    }

    /**
     * Executes the given deeplink without showing a confirmation dialog.
     */
    fun executeRawDeeplink(context: Context, deeplink: DeepLink, isQrData: Boolean = true) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> showAddBaseNodeDialog(context, deeplink)
            is DeepLink.Contacts -> addContactsAction(deeplink.data(), isQrData)
            is DeepLink.Send -> sendAction(deeplink, isQrData)
            is DeepLink.UserProfile -> addContactsAction(deeplink.data()?.let { listOf(it) } ?: emptyList(), isQrData)
            is DeepLink.TorBridges -> addTorBridges(deeplink, isQrData)
            is DeepLink.PaperWallet -> showPaperWalletDialog(context, deeplink, isQrData)
        }
    }

    private fun showAddBaseNodeDialog(context: Context, deeplink: DeepLink.AddBaseNode, isQrData: Boolean = true) {
        val baseNode = deeplink.data()
        dialogManager.replace(
            context = context,
            args = ConfirmDialogArgs(
                dialogId = DialogId.DEEPLINK_ADD_BASE_NODE,
                title = resourceManager.getString(R.string.home_custom_base_node_title),
                description = resourceManager.getString(R.string.home_custom_base_node_description),
                cancelButtonText = resourceManager.getString(R.string.home_custom_base_node_no_button),
                confirmButtonText = resourceManager.getString(R.string.common_lets_do_it),
                onConfirm = {
                    dialogManager.dismiss(DialogId.DEEPLINK_ADD_BASE_NODE)
                    addBaseNodeAction(baseNode, isQrData)
                },
            ).getModular(baseNode, resourceManager),
        )
    }

    private fun addUserProfile(context: Context, deeplink: DeepLink.UserProfile, isQrData: Boolean) {
        val contact = DeepLink.Contacts(
            listOf(
                DeepLink.Contacts.DeeplinkContact(
                    alias = deeplink.alias,
                    tariAddress = deeplink.tariAddress,
                )
            )
        )
        showAddContactsDialog(context, contact, isQrData)
    }

    private fun showAddContactsDialog(context: Context, deeplink: DeepLink.Contacts, isQrData: Boolean = true) {
        val contactDtos = deeplink.data()
        if (contactDtos.isEmpty()) return
        val names = contactDtos.joinToString(", ") { it.contactInfo.getAlias().trim() }
        dialogManager.replace(
            context = context,
            args = ModularDialogArgs(
                dialogId = DialogId.DEEPLINK_ADD_CONTACTS,
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.contact_deeplink_title)),
                    BodyModule(resourceManager.getString(R.string.contact_deeplink_message, contactDtos.size.toString()) + ". " + names),
                    ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                        addContactsAction(contactDtos, isQrData)
                        dialogManager.dismiss(DialogId.DEEPLINK_ADD_CONTACTS)
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
                ),
            )
        )
    }

    private fun addTorBridges(deeplink: DeepLink.TorBridges, isQrData: Boolean) {
        deeplink.torConfigurations.forEach {
            torSharedRepository.addTorBridgeConfiguration(it)
        }
    }

    private fun showPaperWalletDialog(context: Context, deeplink: DeepLink.PaperWallet, isQrSata: Boolean = true) {
        dialogManager.replace(
            context = context,
            args = ModularDialogArgs(
                dialogId = DialogId.DEEPLINK_PAPER_WALLET,
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_title)),
                    BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_body)),
                    ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_sweep_funds_button), ButtonStyle.Normal) {
                        dialogManager.dismiss(DialogId.DEEPLINK_PAPER_WALLET)
                        dialogManager.showNotReadyYetDialog(context)
                    },
                    ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_replace_wallet_button), ButtonStyle.Normal) {
                        dialogManager.dismiss(DialogId.DEEPLINK_PAPER_WALLET)
                        showRememberToBackupDialog(context, deeplink)
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
                ),
            )
        )
    }

    private fun showRememberToBackupDialog(context: Context, deeplink: DeepLink.PaperWallet) {
        dialogManager.replace(
            context = context,
            args = ModularDialogArgs(
                dialogId = DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP,
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_title)),
                    BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_body)),
                    ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_yes_button), ButtonStyle.Normal) {
                        dialogManager.dismiss(DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP)
                        goToBackupAction()
                    },
                    ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_no_button), ButtonStyle.Normal) {
                        dialogManager.dismiss(DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP)
                        replaceWalletAction(deeplink)
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
                ),
            )
        )
    }

    private fun DeepLink.AddBaseNode.data(): BaseNodeDto = BaseNodeDto.fromDeeplink(this)

    private fun DeepLink.Contacts.data(): List<ContactDto> = this.contacts.mapNotNull {
        runCatching {
            val tariWalletAddress = TariWalletAddress.fromBase58(it.tariAddress)
            ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = it.alias))
        }.getOrNull()
    }

    private fun DeepLink.Send.data(): ContactDto? = runCatching {
        val tariWalletAddress = TariWalletAddress.fromBase58(this.walletAddress)
        ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = ""))
    }.getOrNull()

    private fun DeepLink.UserProfile.data(): ContactDto? = runCatching {
        val tariWalletAddress = TariWalletAddress.fromBase58(this.tariAddress)
        ContactDto(FFIContactInfo(walletAddress = tariWalletAddress, alias = this.alias))
    }.getOrNull()

    private fun addContactsAction(contacts: List<ContactDto>, isQrData: Boolean) {
        applicationScope.launch(Dispatchers.IO) {
            contactRepository.addContactList(contacts)
        }
    }

    private fun sendAction(deeplink: DeepLink.Send, isQrData: Boolean) {
        navigator.navigate(Navigation.TxListNavigation.ToSendWithDeeplink(deeplink))
    }

    private fun addBaseNodeAction(baseNodeDto: BaseNodeDto, isQrData: Boolean) {
        baseNodesManager.addUserBaseNode(baseNodeDto)
        baseNodesManager.setBaseNode(baseNodeDto)
        walletManager.syncBaseNode()
    }

    private fun goToBackupAction() {
        navigator.let {
            it.toAllSettings()
            it.toBackupSettings(true)
        }
    }

    private fun replaceWalletAction(deeplink: DeepLink.PaperWallet) {
        walletManager.deleteWallet()
        navigator.navigate(Navigation.SplashScreen(deeplink.seedWords))
    }
}