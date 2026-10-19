package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.TariNavigator
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.airdrop.VerifiedAirdropAccount
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.di.CoroutinesDispatchersModule
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs.DialogId
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DeeplinkManager @Inject constructor(
    private val contactRepository: ContactsRepository,
    private val resourceManager: ResourceManager,
    private val walletManager: WalletManager,
    private val navigator: TariNavigator,
    private val deeplinkParser: DeeplinkParser,
    private val airdropRepository: AirdropRepository,
    private val dialogManager: DialogManager,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:Named(CoroutinesDispatchersModule.MAIN) private val mainDispatcher: CoroutineDispatcher,
) {

    @VisibleForTesting
    internal var currentTimeMillis: () -> Long = { System.currentTimeMillis() }

    // App-wide so the airdrop login confirmation survives the screen that received the deeplink; shown by CommonActivity
    val airdropLoginDialog = SingleLiveEvent<ModularDialogArgs>()

    // The airdrop login confirmation in progress (start time and id); accessed on the main thread only
    private var airdropLoginPendingSince: Long? = null
    private var airdropLoginSessionId = 0

    fun parseDeepLink(deepLink: Uri): DeepLink? = deeplinkParser.parse(deepLink)

    fun parseDeepLink(deepLink: String): DeepLink? = runCatching { deepLink.trim().toUri() }.getOrNull()?.let { deeplinkParser.parse(it) }

    fun getDeeplinkString(deeplink: DeepLink): String = deeplinkParser.toDeeplink(deeplink)

    fun execute(dialogHandler: DialogHandler, deeplink: DeepLink) {
        when (deeplink) {
            is DeepLink.Contacts -> showAddContactsDialog(dialogHandler, deeplink)
            is DeepLink.Send -> sendAction(deeplink)
            is DeepLink.UserProfile -> showUserProfileDialog(dialogHandler, deeplink)
            is DeepLink.PaperWallet -> showPaperWalletDialog(dialogHandler, deeplink)
            is DeepLink.AirdropLoginToken -> handleAirdropTokenAction(dialogHandler, deeplink)
            is DeepLink.Raw -> {
                // Raw deeplinks are handled by individual ViewModels via handleDeeplink()
                // No default action required here
            }
        }
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
        val names = contactDtos.joinToString(", ") { it.alias.orEmpty().trim() }
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

    private fun DeepLink.Contacts.data(): List<Contact> = this.contacts.mapNotNull {
        runCatching {
            val tariWalletAddress = TariWalletAddress.fromBase58(it.tariAddress)
            Contact(walletAddress = tariWalletAddress, alias = it.alias)
        }.getOrNull()
    }

    private fun addContactsAction(contacts: List<Contact>) {
        applicationScope.launch(Dispatchers.IO) {
            contactRepository.addContactList(contacts)
        }
    }

    private fun sendAction(deeplink: DeepLink.Send) {
        walletManager.walletInstance?.getWalletAddress()
        val address = TariWalletAddress.fromBase58(deeplink.walletAddress)
        val contact = contactRepository.findOrCreateContact(address)

        navigator.navigateSequence(
            Navigation.BackToHome,
            Navigation.TxSend.Send(contact, deeplink.amount, deeplink.note),
        )
    }

    private fun goToBackupAction() {
        navigator.navigateSequence(
            Navigation.TxList.ToAllSettings,
            Navigation.AllSettings.BackupSettings(true),
        )
    }

    private fun replaceWalletAction(seedWords: List<String>) {
        walletManager.deleteWallet()
        navigator.navigate(Navigation.SplashScreen(seedWords))
    }

    private fun handleAirdropTokenAction(dialogHandler: DialogHandler, deeplink: DeepLink.AirdropLoginToken) {
        val decision = AirdropLoginDeeplinkPolicy.decide(
            pendingSince = airdropRepository.getLoginPendingSince(),
            now = currentTimeMillis(),
            token = deeplink.token,
            refreshToken = deeplink.refreshToken,
        )
        // The pending login marker is single-use, so clear it whenever a valid token arrives
        when (decision) {
            AirdropLoginDeeplinkPolicy.Decision.IGNORE -> Unit
            AirdropLoginDeeplinkPolicy.Decision.SAVE -> {
                airdropRepository.clearLoginPending()
                airdropRepository.saveAirdropToken(deeplink.token, deeplink.refreshToken)
                dialogHandler.showSimpleDialog(
                    title = resourceManager.getString(R.string.airdrop_login_deeplink_connected_title),
                    description = resourceManager.getString(R.string.airdrop_login_deeplink_connected_message),
                )
            }

            AirdropLoginDeeplinkPolicy.Decision.CONFIRM -> {
                airdropRepository.clearLoginPending()
                // Only one confirmation at a time, so a second link can't swap the account under the user
                val now = currentTimeMillis()
                if (!AirdropLoginDeeplinkPolicy.isConfirmationPending(airdropLoginPendingSince, now)) {
                    // Close a stale confirmation that may still be on screen so it can't be updated in place
                    for (dialogId in listOf(DialogId.DEEPLINK_AIRDROP_LOGIN, DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT)) {
                        if (dialogManager.isDialogShowing(dialogId)) dialogManager.dismiss(dialogId, forceDismiss = true)
                    }
                    airdropLoginPendingSince = now
                    airdropLoginSessionId++
                    showAirdropLoginWarningDialog(deeplink, airdropLoginSessionId)
                }
            }
        }
    }

    private fun showAirdropLoginWarningDialog(deeplink: DeepLink.AirdropLoginToken, sessionId: Int) {
        var lookupRequested = false
        var message = resourceManager.getString(R.string.airdrop_login_deeplink_message)
        if (airdropRepository.airdropToken.value != null) {
            message += "\n\n" + resourceManager.getString(R.string.airdrop_login_deeplink_replace_warning)
        }
        airdropLoginDialog.postValue(
            ModularDialogArgs(
                dialogArgs = DialogArgs(onDismiss = { if (!lookupRequested) clearAirdropLoginPending(sessionId) }),
                dialogId = DialogId.DEEPLINK_AIRDROP_LOGIN,
                filterObscuredTouches = true,
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.airdrop_login_deeplink_title)),
                    BodyModule(message),
                    ButtonModule(resourceManager.getString(R.string.airdrop_login_deeplink_show_account), ButtonStyle.Normal) {
                        // A dialog left over from an older confirmation only closes
                        if (sessionId == airdropLoginSessionId) {
                            lookupRequested = true
                            applicationScope.launch {
                                val account = airdropRepository.verifyAccount(deeplink.refreshToken)
                                withContext(mainDispatcher) {
                                    if (sessionId == airdropLoginSessionId) showAirdropLoginAccountDialog(account, sessionId)
                                }
                            }
                        }
                        dialogManager.dismiss(DialogId.DEEPLINK_AIRDROP_LOGIN)
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
                ),
            )
        )
    }

    // An account that could not be verified can only be cancelled
    private fun showAirdropLoginAccountDialog(account: VerifiedAirdropAccount?, sessionId: Int) {
        var message = if (account != null) {
            resourceManager.getString(R.string.airdrop_login_deeplink_account, account.name)
        } else {
            resourceManager.getString(R.string.airdrop_login_deeplink_unverified)
        }
        if (account != null && airdropRepository.airdropToken.value != null) {
            message += "\n\n" + resourceManager.getString(R.string.airdrop_login_deeplink_replace_warning)
        }
        airdropLoginDialog.postValue(
            ModularDialogArgs(
                dialogArgs = DialogArgs(onDismiss = { clearAirdropLoginPending(sessionId) }),
                dialogId = DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT,
                filterObscuredTouches = true,
                modules = listOfNotNull(
                    HeadModule(resourceManager.getString(R.string.airdrop_login_deeplink_title)),
                    BodyModule(message),
                    account?.let {
                        ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Normal) {
                            if (sessionId == airdropLoginSessionId) airdropRepository.saveAirdropToken(account.token, account.refreshToken)
                            dialogManager.dismiss(DialogId.DEEPLINK_AIRDROP_LOGIN_ACCOUNT)
                        }
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
                ),
            )
        )
    }

    // A late dismiss of an older, expired confirmation must not clear the one that replaced it
    private fun clearAirdropLoginPending(sessionId: Int) {
        if (airdropLoginSessionId == sessionId) airdropLoginPendingSince = null
    }
}