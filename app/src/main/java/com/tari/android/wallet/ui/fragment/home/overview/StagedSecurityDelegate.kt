package com.tari.android.wallet.ui.fragment.home.overview

import android.text.SpannableString
import android.text.Spanned
import com.tari.android.wallet.R
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager.StagedSecurityEffect
import com.tari.android.wallet.data.sharedPrefs.securityStages.WalletSecurityStage
import com.tari.android.wallet.extension.takeIfIs
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.securityStages.SecurityStageHeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item.BackupOnboardingArgs
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModule
import yat.android.ui.extension.HtmlHelper

class StagedSecurityDelegate(
    private val dialogHandler: DialogHandler,
    private val stagedWalletSecurityManager: StagedWalletSecurityManager,
    private val resourceManager: ResourceManager,
    private val tariNavigator: TariNavigator,
) {
    fun handleStagedSecurity(balanceInfo: BalanceInfo) {
        stagedWalletSecurityManager.handleBalanceChange(balanceInfo)
            .takeIfIs<StagedSecurityEffect.ShowStagedSecurityPopUp>()
            ?.let { effect ->
                when (effect.stage) {
                    WalletSecurityStage.Stage1A -> showStagePopUp1A()
                    WalletSecurityStage.Stage1B -> showStagePopUp1B()
                    WalletSecurityStage.Stage2 -> showStagePopUp2()
                    WalletSecurityStage.Stage3 -> showStagePopUp3()
                }
            }
    }

    private fun showStagePopUp1A() {
        showPopup(
            stage = BackupOnboardingArgs.StageOne(resourceManager, this::openStage1),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_1a_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_1a_subtitle),
            body = null,
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_1a_buttons_positive),
            bodyHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.staged_wallet_security_stages_1a_message)),
            positiveAction = { openStage1() },
        )
    }

    private fun showStagePopUp1B() {
        showPopup(
            stage = BackupOnboardingArgs.StageTwo(resourceManager, this::openStage1B),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_1b_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_1b_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_1b_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_1b_buttons_positive),
            positiveAction = { openStage1() },
        )
    }

    private fun showStagePopUp2() {
        showPopup(
            stage = BackupOnboardingArgs.StageThree(resourceManager, this::openStage2),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_2_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_2_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_2_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_2_buttons_positive),
            positiveAction = { openStage2() },
        )
    }

    private fun showStagePopUp3() {
        showPopup(
            stage = BackupOnboardingArgs.StageFour(resourceManager, this::openStage3),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_3_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_3_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_3_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_3_buttons_positive),
            positiveAction = { openStage3() },
        )
    }

    private fun openStage1() {
        dialogHandler.hideDialog()
        tariNavigator.navigateSequence(
            Navigation.TxList.ToAllSettings,
            Navigation.AllSettings.ToBackupSettings(false),
            Navigation.BackupSettings.ToWalletBackupWithRecoveryPhrase,
        )
    }

    private fun openStage1B() {
        dialogHandler.hideDialog()
        tariNavigator.navigateSequence(
            Navigation.TxList.ToAllSettings,
            Navigation.AllSettings.ToBackupSettings(true),
        )
    }

    private fun openStage2() {
        dialogHandler.hideDialog()
        tariNavigator.navigateSequence(
            Navigation.TxList.ToAllSettings,
            Navigation.AllSettings.ToBackupSettings(false),
            Navigation.BackupSettings.ToChangePassword,
        )
    }

    private fun openStage3() {
        dialogHandler.hideDialog()
        // do nothing for now
    }

    private fun showPopup(
        stage: BackupOnboardingArgs,
        titleEmoji: String,
        title: String,
        body: String?,
        positiveButtonTitle: String,
        bodyHtml: Spanned? = null,
        positiveAction: () -> Unit = {},
    ) {
        dialogHandler.showModularDialog(
            SecurityStageHeadModule(titleEmoji, title) { showBackupInfo(stage) },
            BodyModule(body, bodyHtml?.let { SpannableString(it) }),
            ButtonModule(positiveButtonTitle, ButtonStyle.Normal) { positiveAction.invoke() },
            ButtonModule(resourceManager.getString(R.string.staged_wallet_security_buttons_remind_me_later), ButtonStyle.Close),
        )
    }

    private fun showBackupInfo(stage: BackupOnboardingArgs) {
        dialogHandler.showModularDialog(
            BackupOnboardingFlowItemModule(stage),
            SpaceModule(20),
        )
    }
}