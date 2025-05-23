package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import androidx.core.net.toUri
import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.data.contacts.model.FFIContactInfo
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.util.DebugConfig
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
    private val walletManager: WalletManager,
    private val navigator: TariNavigator,
    private val deeplinkParser: DeeplinkParser,
    private val airdropRepository: AirdropRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    fun parseDeepLink(deepLink: Uri): DeepLink? = deeplinkParser.parse(deepLink)

    fun parseDeepLink(deepLink: String): DeepLink? = runCatching { deepLink.trim().toUri() }.getOrNull()?.let { deeplinkParser.parse(it) }

    fun getDeeplinkString(deeplink: DeepLink): String = deeplinkParser.toDeeplink(deeplink)

    fun execute(dialogHandler: DialogHandler, deeplink: DeepLink) {
        when (deeplink) {
            is DeepLink.AddBaseNode -> showAddBaseNodeDialog(dialogHandler, deeplink)
            is DeepLink.Contacts -> showAddContactsDialog(dialogHandler, deeplink)
            is DeepLink.Send -> sendAction(deeplink)
            is DeepLink.UserProfile -> showUserProfileDialog(dialogHandler, deeplink)
            is DeepLink.TorBridges -> addTorBridges(deeplink)
            is DeepLink.PaperWallet -> showPaperWalletDialog(dialogHandler, deeplink)
            is DeepLink.AirdropLoginToken -> handleAirdropTokenAction(deeplink)
        }
    }

    private fun showAddBaseNodeDialog(dialogHandler: DialogHandler, deeplink: DeepLink.AddBaseNode) {
        val baseNode = deeplink.data()
        dialogHandler.showModularDialog(
            ConfirmDialogArgs(
                dialogId = DialogId.DEEPLINK_ADD_BASE_NODE,
                title = resourceManager.getString(R.string.home_custom_base_node_title),
                description = resourceManager.getString(R.string.home_custom_base_node_description),
                cancelButtonText = resourceManager.getString(R.string.home_custom_base_node_no_button),
                confirmButtonText = resourceManager.getString(R.string.common_lets_do_it),
                onConfirm = {
                    dialogHandler.hideDialog(DialogId.DEEPLINK_ADD_BASE_NODE)
                    addBaseNodeAction(dialogHandler, baseNode)
                },
            ).getModular(baseNode, resourceManager)
        )
    }

    private fun showUserProfileDialog(dialogHandler: DialogHandler, deeplink: DeepLink.UserProfile) {
        val contact = DeepLink.Contacts(
            listOf(
                DeepLink.Contacts.DeeplinkContact(
                    alias = deeplink.alias,
                    tariAddress = deeplink.tariAddress,
                )
            )
        )
        showAddContactsDialog(dialogHandler, contact)
    }

    private fun showAddContactsDialog(dialogHandler: DialogHandler, deeplink: DeepLink.Contacts) {
        val contactDtos = deeplink.data()
        if (contactDtos.isEmpty()) return
        val names = contactDtos.joinToString(", ") { it.contactInfo.getAlias().trim() }
        dialogHandler.showModularDialog(
            dialogId = DialogId.DEEPLINK_ADD_CONTACTS,
            HeadModule(resourceManager.getString(R.string.contact_deeplink_title)),
            BodyModule(resourceManager.getString(R.string.contact_deeplink_message, contactDtos.size.toString()) + ". " + names),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                addContactsAction(contactDtos)
                dialogHandler.hideDialog(DialogId.DEEPLINK_ADD_CONTACTS)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
    }

    private fun addTorBridges(deeplink: DeepLink.TorBridges) {
        deeplink.torConfigurations.forEach {
            torSharedRepository.addTorBridgeConfiguration(it)
        }
    }

    private fun showPaperWalletDialog(dialogHandler: DialogHandler, deeplink: DeepLink.PaperWallet) {
        dialogHandler.showModularDialog(
            dialogId = DialogId.DEEPLINK_PAPER_WALLET,
            *listOfNotNull(
                HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_title)),
                BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_body)),
                ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_sweep_funds_button), ButtonStyle.Normal) {
                    dialogHandler.hideDialog(DialogId.DEEPLINK_PAPER_WALLET)
                    dialogHandler.showNotReadyYetDialog()
                }.takeIf { DebugConfig.sweepFundsButtonEnabled },
                ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_replace_wallet_button), ButtonStyle.Normal) {
                    dialogHandler.hideDialog(DialogId.DEEPLINK_PAPER_WALLET)
                    showRememberToBackupDialog(dialogHandler, deeplink)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
            ).toTypedArray(),
        )
    }

    private fun showRememberToBackupDialog(dialogHandler: DialogHandler, deeplink: DeepLink.PaperWallet) {
        dialogHandler.showModularDialog(
            dialogId = DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP,
            HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_title)),
            BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_body)),
            ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_yes_button), ButtonStyle.Normal) {
                dialogHandler.hideDialog(DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP)
                goToBackupAction()
            },
            ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_remember_backup_no_button), ButtonStyle.Normal) {
                dialogHandler.hideDialog(DialogId.DEEPLINK_PAPER_WALLET_REMEMBER_TO_BACKUP)
                showEnterPassphraseDialog(dialogHandler, deeplink)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
        )
    }

    private fun showEnterPassphraseDialog(dialogHandler: DialogHandler, deeplink: DeepLink.PaperWallet) {
        var saveAction: () -> Boolean = { false }

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.restore_wallet_paper_wallet_enter_passphrase_title),
            rightButtonTitle = resourceManager.getString(R.string.common_done),
            rightButtonAction = { saveAction() },
        )

        val bodyModule = BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_enter_passphrase_body))

        val passphraseModule = InputModule(
            value = "",
            hint = resourceManager.getString(R.string.restore_wallet_paper_wallet_enter_passphrase_hint),
            isFirst = true,
            isEnd = true,
            onDoneAction = { saveAction() },
        )

        saveAction = {
            val seeds = deeplink.seedWords(passphraseModule.value)
            if (seeds != null) {
                dialogHandler.hideDialog(DialogId.DEEPLINK_PAPER_WALLET_ENTER_PASSPHRASE)
                replaceWalletAction(seeds)
            } else {
                showPaperWalletErrorDialog(dialogHandler)
            }
            true
        }

        dialogHandler.showInputModalDialog(
            dialogId = DialogId.DEEPLINK_PAPER_WALLET_ENTER_PASSPHRASE,
            headModule,
            bodyModule,
            passphraseModule,
        )
    }

    private fun showPaperWalletErrorDialog(dialogHandler: DialogHandler) {
        dialogHandler.showModularDialog(
            HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_error_title)),
            BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_error_body)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
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

    private fun addContactsAction(contacts: List<ContactDto>) {
        applicationScope.launch(Dispatchers.IO) {
            contactRepository.addContactList(contacts)
        }
    }

    private fun sendAction(deeplink: DeepLink.Send) {
        walletManager.walletInstance?.getWalletAddress()
        val address = TariWalletAddress.fromBase58(deeplink.walletAddress)
        val contact = contactRepository.getContactByAddress(address)

        navigator.navigateSequence(
            Navigation.BackToHome,
            Navigation.TxList.ToSendTariToUser(contact, deeplink.amount, deeplink.note),
        )
    }

    private fun addBaseNodeAction(dialogHandler: DialogHandler, baseNodeDto: BaseNodeDto) {
        if (DebugConfig.selectBaseNodeEnabled) {
            baseNodesManager.addUserBaseNode(baseNodeDto)
            baseNodesManager.setBaseNode(baseNodeDto)
            walletManager.syncBaseNode()
        } else {
            dialogHandler.showNotReadyYetDialog()
        }
    }

    private fun goToBackupAction() {
        navigator.navigateSequence(
            Navigation.TxList.ToAllSettings,
            Navigation.AllSettings.ToBackupSettings(true),
        )
    }

    private fun replaceWalletAction(seedWords: List<String>) {
        walletManager.deleteWallet()
        navigator.navigate(Navigation.SplashScreen(seedWords))
    }

    private fun handleAirdropTokenAction(deeplink: DeepLink.AirdropLoginToken) {
        airdropRepository.saveAirdropToken(deeplink.token, deeplink.refreshToken)
    }
}