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
package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.all_settings_back_up_status_processing
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.databinding.FragmentWalletBackupSettingsBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState.BackupUpToDate
import com.tari.android.wallet.ui.activity.settings.BackupSettingsRouter
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import javax.inject.Inject

internal class BackupSettingsFragment : CommonFragment<FragmentWalletBackupSettingsBinding, BackupSettingsViewModel>() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    @Inject
    lateinit var backupManager: BackupManager

    private var optionsAnimation: Animator? = null

    private val biometricAuthenticationViewModel: BiometricAuthenticationViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWalletBackupSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BackupSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        BiometricAuthenticationViewModel.bindToFragment(biometricAuthenticationViewModel, this)
        viewModel.biometricAuthenticationViewModel = biometricAuthenticationViewModel

        setupUI()
        subscribeUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setSeedWordVerificationStateIcon()
    }

    private fun setupUI() {
        setupViews()
        setupCTAs()
    }

    private fun setupViews() = with(ui) {
        cloudBackupStatusProgressView.setColor(color(all_settings_back_up_status_processing))
        initBackupOptions()
    }

    private fun setupCTAs() = with(ui) {
        backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        backupWithRecoveryPhraseCtaView.setOnClickListener(ThrottleClick { viewModel.onBackupWithRecoveryPhrase() })
        backupWalletToCloudCtaView.setOnClickListener(ThrottleClick { viewModel.onBackupToCloud() })
        updatePasswordCtaView.setOnClickListener(ThrottleClick { viewModel.onUpdatePassword() })
    }

    private fun subscribeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(cloudBackupStatus) { processCloudBackupStatus(it) }

        observe(backupStateChanged) { resetStatusIcons() }

        observe(isBackupAvailable) { onChangeIsBackupAvailable(it) }

        observe(backupWalletToCloudEnabled) { ui.backupWalletToCloudCtaView.isEnabled = it }

        observe(isBackupNowEnabled) { ui.backupNowTextView.alpha = if (it) ALPHA_VISIBLE else ALPHA_DISABLED }

        observe(backupPassword) { updatePasswordChangeLabel(if (it.isPresent) it.get() else null) }

        observe(updatePasswordEnabled) { onChangeUpdatePasswordEnabled(it) }

        observe(lastSuccessfulBackupDate) { updateLastSuccessfulBackupDate(if (it.isPresent) it.get() else null) }

//        observe(showBackupsWillBeDeletedDialog) { showBackupsWillBeDeletedDialog(it.onAccept, it.onDismiss) }

        observe(backupOptionsVisibility) { if (it) showBackupOptionsWithAnimation() else hideAllBackupOptionsWithAnimation() }

        observeOnLoad(backupOptionsAreVisible)

        observeOnLoad(folderSelectionWasSuccessful)
    }

    private fun processNavigation(navigation: BackupSettingsNavigation) {
        val router = requireActivity() as BackupSettingsRouter
        when (navigation) {
            BackupSettingsNavigation.ToChangePassword -> router.toChangePassword(this)
            BackupSettingsNavigation.ToConfirmPassword -> router.toConfirmPassword(this)
            BackupSettingsNavigation.ToWalletBackupWithRecoveryPhrase -> router.toWalletBackupWithRecoveryPhrase(this)
        }
    }

    private fun initBackupOptions() {
        val googleDriveViewModel = BackupOptionViewModel()
        val dropboxViewModel = BackupOptionViewModel()

        ui.googleDriveBackup.viewLifecycle = viewLifecycleOwner
        ui.dropboxBackup.viewLifecycle = viewLifecycleOwner

        ui.googleDriveBackup.init(this, googleDriveViewModel.apply { setup(BackupOptions.Google) })
        ui.dropboxBackup.init(this, dropboxViewModel.apply { setup(BackupOptions.Dropbox) })

        viewModel.setupWithOptions(listOf(googleDriveViewModel, dropboxViewModel))

        if (viewModel.backupSettingsRepository.getOptionList.any { it.isEnable }) {
            if (EventBus.backupState.publishSubject.value?.backupsState is BackupUpToDate) {
                ui.backupWalletToCloudCtaContainerView.gone()
            }
        } else {
            hideAllBackupOptions()
            ui.lastBackupTimeTextView.gone()
        }
    }

    private fun hideAllBackupOptions() {
        if (viewModel.backupOptionsAreVisible.value!!) {
            arrayOf(ui.updatePasswordCtaView, ui.backupWalletToCloudCtaContainerView, ui.lastBackupTimeTextView).forEach(View::gone)
            viewModel.backupOptionsAreVisible.postValue(false)
        }
    }

    private fun onChangeIsBackupAvailable(isAvailable: Boolean) {
        if (isAvailable) animateBackupButtonAvailability() else animateBackupButtonUnavailability()
    }

    private fun onChangeUpdatePasswordEnabled(isEnabled: Boolean) = with(ui) {
        updatePasswordCtaView.isEnabled = isEnabled
        updatePasswordLabelTextView.alpha = if (isEnabled) ALPHA_VISIBLE else ALPHA_DISABLED
        updatePasswordArrowImageView.alpha = if (isEnabled) ALPHA_VISIBLE else ALPHA_DISABLED
    }

    private fun resetStatusIcons() = with(ui) {
        cloudBackupStatusProgressView.invisible()
        cloudBackupStatusSuccessView.invisible()
        cloudBackupStatusWarningView.invisible()
        setSeedWordVerificationStateIcon()
    }

    private fun setSeedWordVerificationStateIcon() = with(ui) {
        val hasVerifiedSeedWords = tariSettingsSharedRepository.hasVerifiedSeedWords
        backupWithRecoveryPhraseSuccessView.setVisible(hasVerifiedSeedWords)
        backupWithRecoveryPhraseWarningView.setVisible(!hasVerifiedSeedWords)
    }

    private fun updatePasswordChangeLabel(password: String?) {
        val textId = if (password == null) back_up_wallet_set_backup_password_cta else back_up_wallet_change_backup_password_cta
        ui.updatePasswordLabelTextView.text = string(textId)
    }

    private fun updateLastSuccessfulBackupDate(lastSuccessfulBackupDate: DateTime?) {
        ui.lastBackupTimeTextView.visible()
        if (lastSuccessfulBackupDate == null) {
            ui.lastBackupTimeTextView.text = ""
        } else {
            val date = lastSuccessfulBackupDate.toLocalDateTime()
            ui.lastBackupTimeTextView.text = string(
                back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(date),
                BACKUP_TIME_FORMATTER.print(date)
            )
        }
    }

    private fun processCloudBackupStatus(status: CloudBackupStatus) {
        val view = when (status) {
            CloudBackupStatus.Success -> ui.cloudBackupStatusSuccessView
            is CloudBackupStatus.InProgress -> ui.cloudBackupStatusProgressView
            CloudBackupStatus.Scheduled -> ui.cloudBackupStatusScheduledView
            is CloudBackupStatus.Warning -> ui.cloudBackupStatusWarningView
        }
        activateBackupStatusView(view, status.text, status.color)
    }


    private fun activateBackupStatusView(icon: View?, textId: Int = -1, textColor: Int = -1) = with(ui) {
        val isVisible = icon == null
        cloudBackupStatusProgressView.setVisible(isVisible, View.INVISIBLE)
        cloudBackupStatusSuccessView.setVisible(isVisible, View.INVISIBLE)
        cloudBackupStatusWarningView.setVisible(isVisible, View.INVISIBLE)
        cloudBackupStatusScheduledView.setVisible(isVisible, View.INVISIBLE)
        val hideText = textId == -1
        backupStatusTextView.text = if (hideText) "" else string(textId)
        backupStatusTextView.setVisible(!hideText)
        if (textColor != -1) backupStatusTextView.setTextColor(color(textColor))
    }

    //todo refactor to nice state
    private fun showBackupsWillBeDeletedDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
        BottomSlideDialog(
            requireContext(),
            R.layout.dialog_turn_off_backups_will_be_deleted_warning,
            canceledOnTouchOutside = false
        ).apply {
            findViewById<View>(R.id.backup_turn_off_confirm_button)
                .setOnClickListener(ThrottleClick {
                    onAccept()
                    dismiss()
                })
            findViewById<View>(R.id.backup_turn_off_cancel_button)
                .setOnClickListener(ThrottleClick {
                    onDismiss()
                    dismiss()
                })
        }.show()
    }

    // region Backup button animations
    private fun animateBackupButtonAvailability() {
        val animation = optionsAnimation
        if (viewModel.backupOptionsAreVisible.value == true &&
            ui.backupWalletToCloudCtaContainerView.visibility != View.VISIBLE &&
            (animation == null || !animation.isRunning)
        ) {
            optionsAnimation = ValueAnimator.ofFloat(ALPHA_INVISIBLE, ALPHA_VISIBLE).apply {
                duration = OPTIONS_ANIMATION_DURATION
                interpolator = LinearInterpolator()
                addUpdateListener { ui.backupWalletToCloudCtaContainerView.alpha = it.animatedValue as Float }
                addListener(
                    onStart = {
                        ui.backupWalletToCloudCtaContainerView.alpha = ALPHA_INVISIBLE
                        ui.backupWalletToCloudCtaContainerView.visible()
                    }
                )
                start()
            }
        }
    }

    private fun animateBackupButtonUnavailability() {
        val animation = optionsAnimation
        if (ui.backupWalletToCloudCtaContainerView.visibility != View.GONE && (animation == null || !animation.isRunning)) {
            optionsAnimation = ValueAnimator.ofFloat(ALPHA_VISIBLE, ALPHA_INVISIBLE).apply {
                duration = OPTIONS_ANIMATION_DURATION
                interpolator = LinearInterpolator()
                addUpdateListener { ui.backupWalletToCloudCtaContainerView.alpha = it.animatedValue as Float }
                addListener(
                    onStart = { ui.backupWalletToCloudCtaContainerView.alpha = ALPHA_VISIBLE },
                    onEnd = { ui.backupWalletToCloudCtaContainerView.gone() },
                    onCancel = { ui.backupWalletToCloudCtaContainerView.gone() }
                )
                start()
            }
        }
    }
    // endRegion


    // region Backup options animations

    private fun showBackupOptionsWithAnimation() {
        if (viewModel.backupOptionsAreVisible.value!!) return
        val views = arrayOf(
            ui.updatePasswordCtaView,
            ui.lastBackupTimeTextView,
            ui.backupWalletToCloudCtaContainerView
        )
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_INVISIBLE, ALPHA_VISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.visible()
                        v.alpha = ALPHA_INVISIBLE
                    }
                },
                onCancel = {
                    views.forEach { v -> v.alpha = ALPHA_VISIBLE }
                    if (EventBus.backupState.publishSubject.value?.backupsState is BackupUpToDate) {
                        animateBackupButtonUnavailability()
                    }
                },
                onEnd = {
                    if (EventBus.backupState.publishSubject.value?.backupsState is BackupUpToDate) {
                        animateBackupButtonUnavailability()
                    }
                }
            )
            start()
        }
        viewModel.backupOptionsAreVisible.postValue(true)
    }

    private fun hideAllBackupOptionsWithAnimation() {
        if (!viewModel.backupOptionsAreVisible.value!!) return
        val views = arrayOf(ui.updatePasswordCtaView, ui.backupWalletToCloudCtaContainerView, ui.lastBackupTimeTextView)
        val wasClickable = views.map { it.isClickable }
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_VISIBLE, ALPHA_INVISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            val finalizeAnimation: (Animator?) -> Unit = {
                views.zip(wasClickable).forEach { (view, wasClickable) ->
                    view.isClickable = wasClickable
                    view.gone()
                    view.alpha = ALPHA_VISIBLE
                }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.isClickable = false
                        v.alpha = ALPHA_VISIBLE
                    }
                },
                onEnd = finalizeAnimation,
                onCancel = finalizeAnimation
            )
            start()
        }
        viewModel.backupOptionsAreVisible.postValue(false)
    }
    // endRegion

    companion object {
        fun newInstance() = BackupSettingsFragment()

        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
        private const val OPTIONS_ANIMATION_DURATION = 500L
        private const val ALPHA_INVISIBLE = 0F
        private const val ALPHA_VISIBLE = 1F
        private const val ALPHA_DISABLED = 0.15F
    }
}