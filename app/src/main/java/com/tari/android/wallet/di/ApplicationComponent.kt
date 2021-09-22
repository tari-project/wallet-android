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

import android.content.Context
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.service.BootDeviceReceiver
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.AuthActivity
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.debug.DebugActivity
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.ui.activity.qr.QRScannerActivity
import com.tari.android.wallet.ui.activity.restore.WalletRestoreActivity
import com.tari.android.wallet.ui.activity.send.SendTariActivity
import com.tari.android.wallet.ui.activity.settings.DeleteWalletActivity
import com.tari.android.wallet.ui.activity.tx.TxDetailsActivity
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.networkStateIndicator.ConnectionIndicatorViewModel
import com.tari.android.wallet.ui.fragment.debug.DebugLogFragment
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.BaseNodeConfigFragment
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.BaseNodeConfigViewModel
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode.AddCustomBaseNodeViewModel
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.ChangeBaseNodeViewModel
import com.tari.android.wallet.ui.fragment.onboarding.CreateWalletFragment
import com.tari.android.wallet.ui.fragment.onboarding.IntroductionFragment
import com.tari.android.wallet.ui.fragment.onboarding.LocalAuthFragment
import com.tari.android.wallet.ui.fragment.profile.WalletInfoFragment
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionFragment
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionViewModel
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordViewModel
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.InputSeedWordsViewModel
import com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords.WalletRestoringFromSeedWordsViewModel
import com.tari.android.wallet.ui.fragment.send.AddAmountFragment
import com.tari.android.wallet.ui.fragment.send.AddRecipientFragment
import com.tari.android.wallet.ui.fragment.send.FinalizeSendTxFragment
import com.tari.android.wallet.ui.fragment.send.addNote.AddNoteFragment
import com.tari.android.wallet.ui.fragment.settings.AllSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backgroundService.BackgroundServiceSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase.VerifySeedPhraseViewModel
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import com.tari.android.wallet.ui.fragment.tx.TxListFragment
import com.tari.android.wallet.ui.fragment.tx.TxListViewModel
import dagger.Component
import java.io.File
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
        TrackerModule::class,
        BackupAndRestoreModule::class,
        PresentationModule::class
    ]
)

internal interface ApplicationComponent {

    val context: Context

    val workingDir: File

    val sharedPrefsWrapper: SharedPrefsRepository

    val authenticationService: BiometricAuthenticationService

    val bugReportingService: BugReportingService

    /**
     * Application.
     */
    fun inject(application: TariWalletApplication)

    /**
     * Activities.
     */
    fun inject(activity: SplashActivity)
    fun inject(activity: OnboardingFlowActivity)
    fun inject(activity: AuthActivity)
    fun inject(activity: HomeActivity)
    fun inject(activity: QRScannerActivity)
    fun inject(activity: SendTariActivity)
    fun inject(activity: TxDetailsActivity)
    fun inject(activity: DebugActivity)
    fun inject(activity: DeleteWalletActivity)

    /**
     * Fragments.
     */
    fun inject(fragment: IntroductionFragment)
    fun inject(fragment: CreateWalletFragment)
    fun inject(fragment: AddRecipientFragment)
    fun inject(fragment: AddAmountFragment)
    fun inject(fragment: AddNoteFragment)
    fun inject(fragment: AddNoteFragment.ChooseGIFDialogFragment)
    fun inject(fragment: FinalizeSendTxFragment)
    fun inject(fragment: LocalAuthFragment)
    fun inject(fragment: DebugLogFragment)
    fun inject(fragment: BaseNodeConfigFragment)
    fun inject(fragment: AllSettingsFragment)
    fun inject(fragment: WalletInfoFragment)
    fun inject(fragment: TxListFragment)
    /**
     * Backup.
     */
    fun inject(fragment: BackupSettingsFragment)
    fun inject(fragment: ChangeSecurePasswordFragment)
    fun inject(fragment: EnterCurrentPasswordFragment)
    fun inject(fragment: ChooseRestoreOptionFragment)
    /**
     * Restore.
     */
    fun inject(activity: WalletRestoreActivity)

    /**
     * ViewModels.
     */
    fun inject(commonViewModel: CommonViewModel)
    fun inject(thumbnailGIFsViewModel: AddNoteFragment.ThumbnailGIFsViewModel)
    fun inject(gifViewModel: TxDetailsActivity.GIFViewModel)
    fun inject(backgroundServiceSettingsViewModel: BackgroundServiceSettingsViewModel)
    fun inject(connectionIndicatorViewModel: ConnectionIndicatorViewModel)
    fun inject(chooseRestoreOptionViewModel: ChooseRestoreOptionViewModel)
    fun inject(enterRestorationPasswordViewModel: EnterRestorationPasswordViewModel)
    fun inject(walletRestoringFromSeedWordsViewModel: WalletRestoringFromSeedWordsViewModel)
    fun inject(inputSeedWordsViewModel: InputSeedWordsViewModel)
    fun inject(verifySeedPhraseViewModel: VerifySeedPhraseViewModel)
    fun inject(backupSettingsViewModel: BackupSettingsViewModel)
    fun inject(biometricAuthenticationViewModel: BiometricAuthenticationViewModel)
    fun inject(txListViewModel: TxListViewModel)
    fun inject(baseNodeConfigViewModel: BaseNodeConfigViewModel)
    fun inject(changeBaseNodeViewModel: ChangeBaseNodeViewModel)
    fun inject(addCustomBaseNodeViewModel: AddCustomBaseNodeViewModel)
    /**
     * Service(s).
     */
    fun inject(service: WalletService)

    /**
    * Broadcast receiver
    */
    fun inject(receiver: BootDeviceReceiver)

}
