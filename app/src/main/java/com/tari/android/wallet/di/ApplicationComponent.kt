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
import com.tari.android.wallet.infrastructure.ShareManager
import com.tari.android.wallet.notification.TariFcmService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.StartActivity
import com.tari.android.wallet.ui.screen.auth.AuthActivity
import com.tari.android.wallet.ui.screen.auth.AuthViewModel
import com.tari.android.wallet.ui.screen.biometrics.ChangeBiometricsViewModel
import com.tari.android.wallet.ui.screen.contactBook.add.AddContactViewModel
import com.tari.android.wallet.ui.screen.contactBook.details.ContactDetailsViewModel
import com.tari.android.wallet.ui.screen.contactBook.list.ContactListViewModel
import com.tari.android.wallet.ui.screen.debug.sampleDesign.SampleDesignSystemViewModel
import com.tari.android.wallet.ui.screen.home.HomeViewModel
import com.tari.android.wallet.ui.screen.home.overview.HomeOverviewViewModel
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowActivity
import com.tari.android.wallet.ui.screen.onboarding.createWallet.CreateWalletFragment
import com.tari.android.wallet.ui.screen.onboarding.createWallet.CreateWalletViewModel
import com.tari.android.wallet.ui.screen.onboarding.inroduction.IntroductionViewModel
import com.tari.android.wallet.ui.screen.onboarding.localAuth.LocalAuthViewModel
import com.tari.android.wallet.ui.screen.pinCode.EnterPinCodeViewModel
import com.tari.android.wallet.ui.screen.profile.login.ProfileLoginViewModel
import com.tari.android.wallet.ui.screen.profile.profile.ProfileViewModel
import com.tari.android.wallet.ui.screen.profile.walletInfo.WalletInfoViewModel
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity
import com.tari.android.wallet.ui.screen.qr.QrScannerViewModel
import com.tari.android.wallet.ui.screen.restore.activity.WalletRestoreViewModel
import com.tari.android.wallet.ui.screen.restore.chooseRestoreOption.ChooseRestoreOptionViewModel
import com.tari.android.wallet.ui.screen.restore.enterRestorationPassword.EnterRestorationPasswordViewModel
import com.tari.android.wallet.ui.screen.restore.inputSeedWords.InputSeedWordsViewModel
import com.tari.android.wallet.ui.screen.restore.walletRestoring.WalletRestoringViewModel
import com.tari.android.wallet.ui.screen.send.confirm.ConfirmViewModel
import com.tari.android.wallet.ui.screen.send.obsolete.finalize.FinalizeSendTxViewModel
import com.tari.android.wallet.ui.screen.send.obsolete.requestTari.RequestTariViewModel
import com.tari.android.wallet.ui.screen.send.receive.ReceiveViewModel
import com.tari.android.wallet.ui.screen.send.send.SendViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.AllSettingsViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.about.TariAboutViewModel
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.BackupSettingsViewModel
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword.ChangeSecurePasswordViewModel
import com.tari.android.wallet.ui.screen.settings.backup.enterCurrentPassword.EnterCurrentPasswordViewModel
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.BackupLearnMoreViewModel
import com.tari.android.wallet.ui.screen.settings.backup.learnMore.item.BackupLearnMoreItemViewModel
import com.tari.android.wallet.ui.screen.settings.backup.verifySeedPhrase.VerifySeedPhraseViewModel
import com.tari.android.wallet.ui.screen.settings.baseNodeConfig.changeBaseNode.ChangeBaseNodeViewModel
import com.tari.android.wallet.ui.screen.settings.bugReporting.BugsReportingViewModel
import com.tari.android.wallet.ui.screen.settings.dataCollection.DataCollectionViewModel
import com.tari.android.wallet.ui.screen.settings.deleteWallet.DeleteWalletViewModel
import com.tari.android.wallet.ui.screen.settings.logs.logFiles.LogFilesViewModel
import com.tari.android.wallet.ui.screen.settings.logs.logs.LogsViewModel
import com.tari.android.wallet.ui.screen.settings.networkSelection.NetworkSelectionViewModel
import com.tari.android.wallet.ui.screen.settings.screenRecording.ScreenRecordingSettingsViewModel
import com.tari.android.wallet.ui.screen.settings.themeSelector.ThemeSelectorViewModel
import com.tari.android.wallet.ui.screen.settings.torBridges.TorBridgesSelectionViewModel
import com.tari.android.wallet.ui.screen.settings.torBridges.customBridges.CustomTorBridgesViewModel
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsViewModel
import com.tari.android.wallet.ui.screen.tx.history.TxHistoryViewModel
import com.tari.android.wallet.ui.screen.utxos.list.UtxosListViewModel
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
        TorModule::class,
        RetrofitModule::class,
        CoroutinesDispatchersModule::class,
    ]
)

interface ApplicationComponent {

    fun inject(application: TariWalletApplication)

    fun inject(activity: StartActivity)
    fun inject(activity: OnboardingFlowActivity)
    fun inject(activity: AuthActivity)
    fun inject(activity: QrScannerActivity)

    fun inject(fragment: CreateWalletFragment)

    fun inject(commonViewModel: CommonViewModel)
    fun inject(viewModel: ScreenRecordingSettingsViewModel)
    fun inject(viewModel: ChooseRestoreOptionViewModel)
    fun inject(viewModel: EnterRestorationPasswordViewModel)
    fun inject(viewModel: WalletRestoringViewModel)
    fun inject(viewModel: InputSeedWordsViewModel)
    fun inject(viewModel: VerifySeedPhraseViewModel)
    fun inject(viewModel: BackupSettingsViewModel)
    fun inject(viewModel: ChangeBaseNodeViewModel)
    fun inject(viewModel: NetworkSelectionViewModel)
    fun inject(viewModel: AllSettingsViewModel)
    fun inject(viewModel: FinalizeSendTxViewModel)
    fun inject(viewModel: WalletInfoViewModel)
    fun inject(viewModel: RequestTariViewModel)
    fun inject(viewModel: TorBridgesSelectionViewModel)
    fun inject(viewModel: CustomTorBridgesViewModel)
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
    fun inject(viewModel: ThemeSelectorViewModel)
    fun inject(viewModel: DeleteWalletViewModel)
    fun inject(viewModel: HomeViewModel)
    fun inject(viewModel: HomeOverviewViewModel)
    fun inject(viewModel: TxHistoryViewModel)
    fun inject(viewModel: EnterCurrentPasswordViewModel)
    fun inject(viewModel: ChangeSecurePasswordViewModel)
    fun inject(viewModel: BackupLearnMoreViewModel)
    fun inject(viewModel: BackupLearnMoreItemViewModel)
    fun inject(viewModel: TxDetailsViewModel)
    fun inject(viewModel: ShareManager)
    fun inject(viewModel: QrScannerViewModel)
    fun inject(viewModel: DataCollectionViewModel)
    fun inject(viewModel: EnterPinCodeViewModel)
    fun inject(viewModel: ChangeBiometricsViewModel)
    fun inject(viewModel: WalletRestoreViewModel)
    fun inject(viewModel: SampleDesignSystemViewModel)
    fun inject(viewModel: ProfileLoginViewModel)
    fun inject(viewModel: ProfileViewModel)
    fun inject(viewModel: ReceiveViewModel)
    fun inject(viewModel: ConfirmViewModel)
    fun inject(viewModel: ContactListViewModel)
    fun inject(viewModel: ContactDetailsViewModel)
    fun inject(viewModel: AddContactViewModel)
    fun inject(viewModel: SendViewModel)

    fun inject(tariFcmService: TariFcmService)

    fun getClipboardManager(): ClipboardManager
}
