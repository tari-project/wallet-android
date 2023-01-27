package com.tari.android.wallet.application.securityStage

import android.text.SpannableString
import android.text.Spanned
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.*
import com.tari.android.wallet.data.sharedPrefs.securityStages.modules.SecurityStageHeadModule
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import yat.android.ui.extension.HtmlHelper
import java.math.BigInteger
import java.util.Calendar
import javax.inject.Inject

class StagedWalletSecurityManager : CommonViewModel() {

    @Inject
    lateinit var securityStagesRepository: SecurityStagesRepository

    @Inject
    lateinit var backupPrefsRepository: BackupSettingsRepository

    @Inject
    lateinit var sharedPrefsRepository: SharedPrefsRepository

    init {
        component.inject(this)

        EventBus.balanceUpdates.publishSubject.subscribe { handleChange(it) }.addTo(compositeDisposable)
    }

    val hasVerifiedSeedPhrase
        get() = tariSettingsSharedRepository.hasVerifiedSeedWords

    val isBackupOn
        get() = backupPrefsRepository.getOptionList.any { it.isEnable }

    val isBackupPasswordSet
        get() = !backupPrefsRepository.backupPassword.isNullOrEmpty()

    val disabledTimestampSinceNow
        get() = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 7) }

    var disabledTimestamps: MutableMap<WalletSecurityStage, Calendar>
        get() = securityStagesRepository.disabledTimestamps?.timestamps ?: DisabledTimestampsDto(mutableMapOf()).timestamps
        set(value) {
            securityStagesRepository.disabledTimestamps = DisabledTimestampsDto(value)
        }

    private fun updateTimestamp(securityStage: WalletSecurityStage) {
        val newTimestamp = disabledTimestampSinceNow
        disabledTimestamps = disabledTimestamps.also { it[securityStage] = newTimestamp }
    }

    private fun handleChange(balance: BalanceInfo) {
        val securityStage = getSecurityStages(balance) ?: return
        //todo Stage 3 is currently disabled
        if (securityStage == WalletSecurityStage.Stage3) return
        if (isActionDisabled(securityStage)) return

        updateTimestamp(securityStage)
        showPopUp(securityStage)
    }

    private fun getSecurityStages(balanceInfo: BalanceInfo): WalletSecurityStage? {
        val balance = balanceInfo.availableBalance

        return when {
            balance >= minimumStageOneBalance && !hasVerifiedSeedPhrase -> WalletSecurityStage.Stage1A
            balance >= minimumStageOneBalance && !isBackupOn -> WalletSecurityStage.Stage1B
            balance >= stageTwoThresholdBalance && !isBackupPasswordSet -> WalletSecurityStage.Stage2
            balance >= safeHotWalletBalance -> WalletSecurityStage.Stage3
            else -> null
        }
    }

    private fun isActionDisabled(securityStage: WalletSecurityStage): Boolean {
        val timestamp = disabledTimestamps[securityStage] ?: return false
        if (timestamp < Calendar.getInstance()) {
            return true
        }

        disabledTimestamps = disabledTimestamps.also { it.remove(securityStage) }
        return false
    }

    fun showPopUp(securityStage: WalletSecurityStage) {
        when (securityStage) {
            WalletSecurityStage.Stage1A -> showStage1APopUp()
            WalletSecurityStage.Stage1B -> showStage1BPopUp()
            WalletSecurityStage.Stage2 -> showStage2PopUp()
            WalletSecurityStage.Stage3 -> showStage3PopUp()
        }
    }

    private fun showStage1APopUp() {
        showPopup(
            resourceManager.getString(R.string.staged_wallet_security_stages_1a_title),
            resourceManager.getString(R.string.staged_wallet_security_stages_1a_subtitle),
            null,
            resourceManager.getString(R.string.staged_wallet_security_stages_1a_buttons_positive),
            HtmlHelper.getSpannedText(resourceManager.getString(R.string.staged_wallet_security_stages_1a_message))
        ) {
            HomeActivity.instance.get()?.let {
                it.toAllSettings()
                it.toBackupSettings(false)
                it.toWalletBackupWithRecoveryPhrase()
            }
        }
    }

    private fun showStage1BPopUp() {
        showPopup(
            resourceManager.getString(R.string.staged_wallet_security_stages_1b_title),
            resourceManager.getString(R.string.staged_wallet_security_stages_1b_subtitle),
            resourceManager.getString(R.string.staged_wallet_security_stages_1b_message),
            resourceManager.getString(R.string.staged_wallet_security_stages_1b_buttons_positive),
        ) {
            HomeActivity.instance.get()?.let {
                it.toAllSettings()
                it.toBackupSettings()
            }
        }
    }

    private fun showStage2PopUp() {
        showPopup(
            resourceManager.getString(R.string.staged_wallet_security_stages_2_title),
            resourceManager.getString(R.string.staged_wallet_security_stages_2_subtitle),
            resourceManager.getString(R.string.staged_wallet_security_stages_2_message),
            resourceManager.getString(R.string.staged_wallet_security_stages_2_buttons_positive),
        ) {
            HomeActivity.instance.get()?.let {
                it.toAllSettings()
                it.toBackupSettings(false)
                it.toChangePassword()
            }
        }
    }

    private fun showStage3PopUp() {
        showPopup(
            resourceManager.getString(R.string.staged_wallet_security_stages_3_title),
            resourceManager.getString(R.string.staged_wallet_security_stages_3_subtitle),
            resourceManager.getString(R.string.staged_wallet_security_stages_3_message),
            resourceManager.getString(R.string.staged_wallet_security_stages_3_buttons_positive),
        )
    }

    private fun showPopup(
        titleEmoji: String,
        title: String,
        body: String?,
        positiveButtonTitle: String,
        bodyHtml: Spanned? = null,
        positiveAction: () -> Unit = {},
    ) {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                SecurityStageHeadModule(titleEmoji, title) {
                    HomeActivity.instance.get()?.let(HomeActivity::toBackupOnboardingFlow)
                },
                BodyModule(body, bodyHtml?.let { SpannableString(it) }),
                ButtonModule(positiveButtonTitle, ButtonStyle.Normal) {
                    _dismissDialog.postValue(Unit)
                    positiveAction.invoke()
                },
                ButtonModule(resourceManager.getString(R.string.staged_wallet_security_buttons_remind_me_later), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    companion object {
        val minimumStageOneBalance = MicroTari(BigInteger.valueOf(10_000))
        val stageTwoThresholdBalance = MicroTari(BigInteger.valueOf(100_000))
        val safeHotWalletBalance = MicroTari(BigInteger.valueOf(500_000_000))
        val maxHotWalletBalance = MicroTari(BigInteger.valueOf(1_000_000_000))
    }
}