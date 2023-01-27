/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.di

import android.content.ClipboardManager
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.service.service.WalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.fragment.auth.AuthActivity
import com.tari.android.wallet.ui.fragment.auth.AuthViewModel
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.home.HomeViewModel
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletViewModel
import com.tari.android.wallet.ui.fragment.onboarding.inroduction.IntroductionViewModel
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthViewModel
import com.tari.android.wallet.ui.fragment.profile.WalletInfoViewModel
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity
import com.tari.android.wallet.ui.fragment.restore.activity.WalletRestoreActivity
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionViewModel
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordViewModel
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.InputSeedWordsViewModel
import com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords.WalletRestoringFromSeedWordsViewModel
import com.tari.android.wallet.ui.fragment.send.addAmount.AddAmountViewModel
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteViewModel
import com.tari.android.wallet.ui.fragment.send.addNote.gif.ChooseGIFDialogFragment
import com.tari.android.wallet.ui.fragment.send.addNote.gif.ThumbnailGIFsViewModel
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.addRecepient.AddRecipientViewModel
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxViewModel
import com.tari.android.wallet.ui.fragment.send.requestTari.RequestTariViewModel
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.allSettings.about.TariAboutViewModel
import com.tari.android.wallet.ui.fragment.settings.backgroundService.BackgroundServiceSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.BackupOnboardingFlowViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item.BackupOnboardingFlowItemViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.changeSecurePassword.ChangeSecurePasswordViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.enterCurrentPassword.EnterCurrentPasswordViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase.VerifySeedPhraseViewModel
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.addBaseNode.AddCustomBaseNodeViewModel
import com.tari.android.wallet.ui.fragment.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeViewModel
import com.tari.android.wallet.ui.fragment.settings.bugReporting.BugsReportingViewModel
import com.tari.android.wallet.ui.fragment.settings.deleteWallet.DeleteWalletViewModel
import com.tari.android.wallet.ui.fragment.settings.logs.LogFilesManager
import com.tari.android.wallet.ui.fragment.settings.logs.logFiles.LogFilesViewModel
import com.tari.android.wallet.ui.fragment.settings.logs.logs.LogsViewModel
import com.tari.android.wallet.ui.fragment.settings.networkSelection.NetworkSelectionViewModel
import com.tari.android.wallet.ui.fragment.settings.themeSelector.ThemeSelectorViewModel
import com.tari.android.wallet.ui.fragment.settings.torBridges.TorBridgesSelectionViewModel
import com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges.CustomTorBridgesViewModel
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import com.tari.android.wallet.ui.fragment.splash.SplashActivity
import com.tari.android.wallet.ui.fragment.tx.TxListViewModel
import com.tari.android.wallet.ui.fragment.tx.details.gif.GIFViewModel
import com.tari.android.wallet.ui.fragment.utxos.list.UtxosListViewModel
import dagger.Component
import javax.inject.Singleton

/**
 * Dagger component that injects objects through modules.
 *
 * @author The Tari Development Team
 */
@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        WalletModule::class,
        ServiceModule::class,
        TorModule::class,
        BackupAndRestoreModule::class,
        PresentationModule::class,
        YatModule::class
    ]
)

interface ApplicationComponent {

    fun inject(application: TariWalletApplication)

    fun inject(service: WalletService)

    fun inject(activity: SplashActivity)
    fun inject(activity: OnboardingFlowActivity)
    fun inject(activity: AuthActivity)
    fun inject(activity: HomeActivity)
    fun inject(activity: QRScannerActivity)
    fun inject(activity: WalletRestoreActivity)

    fun inject(fragment: AddRecipientFragment)
    fun inject(fragment: AddNoteFragment)
    fun inject(fragment: ChooseGIFDialogFragment)

    fun inject(commonViewModel: CommonViewModel)
    fun inject(viewModel: ThumbnailGIFsViewModel)
    fun inject(viewModel: GIFViewModel)
    fun inject(viewModel: BackgroundServiceSettingsViewModel)
    fun inject(viewModel: ConnectionIndicatorViewModel)
    fun inject(viewModel: ChooseRestoreOptionViewModel)
    fun inject(viewModel: EnterRestorationPasswordViewModel)
    fun inject(viewModel: WalletRestoringFromSeedWordsViewModel)
    fun inject(viewModel: InputSeedWordsViewModel)
    fun inject(viewModel: VerifySeedPhraseViewModel)
    fun inject(viewModel: BackupSettingsViewModel)
    fun inject(viewModel: BiometricAuthenticationViewModel)
    fun inject(viewModel: TxListViewModel)
    fun inject(viewModel: ChangeBaseNodeViewModel)
    fun inject(viewModel: AddCustomBaseNodeViewModel)
    fun inject(viewModel: NetworkSelectionViewModel)
    fun inject(viewModel: AllSettingsViewModel)
    fun inject(viewModel: AddRecipientViewModel)
    fun inject(viewModel: FinalizeSendTxViewModel)
    fun inject(viewModel: WalletInfoViewModel)
    fun inject(viewModel: RequestTariViewModel)
    fun inject(viewModel: AddAmountViewModel)
    fun inject(viewModel: TorBridgesSelectionViewModel)
    fun inject(viewModel: CustomTorBridgesViewModel)
    fun inject(viewModel: DeeplinkViewModel)
    fun inject(viewModel: LocalAuthViewModel)
    fun inject(viewModel: CreateWalletViewModel)
    fun inject(viewModel: IntroductionViewModel)
    fun inject(viewModel: AuthViewModel)
    fun inject(viewModel: TariAboutViewModel)
    fun inject(viewModel: UtxosListViewModel)
    fun inject(viewModel: BugsReportingViewModel)
    fun inject(viewModel: LogFilesViewModel)
    fun inject(viewModel: LogsViewModel)
    fun inject(viewModel: BackupOptionViewModel)
    fun inject(viewModel: LogFilesManager)
    fun inject(viewModel: ThemeSelectorViewModel)
    fun inject(viewModel: DeleteWalletViewModel)
    fun inject(viewModel: HomeViewModel)
    fun inject(viewModel: EnterCurrentPasswordViewModel)
    fun inject(viewModel: ChangeSecurePasswordViewModel)
    fun inject(viewModel: AddNoteViewModel)
    fun inject(viewModel: StagedWalletSecurityManager)
    fun inject(viewModel: BackupOnboardingFlowViewModel)
    fun inject(viewModel: BackupOnboardingFlowItemViewModel)

    fun getClipboardManager(): ClipboardManager
}
