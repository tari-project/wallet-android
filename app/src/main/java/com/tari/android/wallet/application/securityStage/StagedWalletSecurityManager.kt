package com.tari.android.wallet.application.securityStage

import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager.StagedSecurityEffect.NoStagedSecurityPopUp
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager.StagedSecurityEffect.ShowStagedSecurityPopUp
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.SecurityStagesPrefRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.WalletSecurityStage
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.util.extension.isAfterNow
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

val MINIMUM_STAGE_ONE_BALANCE = MicroTari((BigDecimal.valueOf(10_000) * MicroTari.precisionValue).toBigInteger())
val STAGE_TWO_THRESHOLD_BALANCE = MicroTari((BigDecimal.valueOf(100_000) * MicroTari.precisionValue).toBigInteger())
val SAFE_HOT_WALLET_BALANCE = MicroTari((BigDecimal.valueOf(500_000_000) * MicroTari.precisionValue).toBigInteger())
val MAX_HOT_WALLET_BALANCE = MicroTari((BigDecimal.valueOf(1_000_000_000) * MicroTari.precisionValue).toBigInteger())

@Singleton
class StagedWalletSecurityManager @Inject constructor(
    private val securityStagesRepository: SecurityStagesPrefRepository,
    private val backupPrefsRepository: BackupPrefRepository,
    private val tariSettingsSharedRepository: TariSettingsPrefRepository,
) {
    private val hasVerifiedSeedPhrase
        get() = tariSettingsSharedRepository.hasVerifiedSeedWords

    private val isBackupOn
        get() = backupPrefsRepository.currentBackupOption.isEnable

    private val isBackupPasswordSet
        get() = !backupPrefsRepository.backupPassword.isNullOrEmpty()

    private val disabledTimestampSinceNow: Calendar
        get() = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, 7) }

    /**
     * Check the current security stage based on the balance and the user's security settings.
     */
    fun handleBalanceChange(balance: BalanceInfo): StagedSecurityEffect {
        val securityStage = checkSecurityStage(balance) ?: return NoStagedSecurityPopUp
        //todo Stage 3 is currently disabled
        if (securityStage == WalletSecurityStage.Stage3) return NoStagedSecurityPopUp
        if (securityStagesRepository.disabledTimestamp.isAfterNow()) return NoStagedSecurityPopUp

        securityStagesRepository.disabledTimestamp = disabledTimestampSinceNow

        return ShowStagedSecurityPopUp(securityStage)
    }

    /**
     * Returns null if no security stage is required.
     */
    private fun checkSecurityStage(balanceInfo: BalanceInfo): WalletSecurityStage? {
        val balance = balanceInfo.availableBalance

        return when {
            balance >= MINIMUM_STAGE_ONE_BALANCE && !hasVerifiedSeedPhrase -> WalletSecurityStage.Stage1A
            balance >= MINIMUM_STAGE_ONE_BALANCE && !isBackupOn -> WalletSecurityStage.Stage1B
            balance >= STAGE_TWO_THRESHOLD_BALANCE && !isBackupPasswordSet -> WalletSecurityStage.Stage2
            balance >= SAFE_HOT_WALLET_BALANCE -> WalletSecurityStage.Stage3
            else -> null
        }
    }

    sealed class StagedSecurityEffect {
        data class ShowStagedSecurityPopUp(val stage: WalletSecurityStage) : StagedSecurityEffect()
        data object NoStagedSecurityPopUp : StagedSecurityEffect()
    }
}