package com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item

import android.graphics.Typeface
import android.text.SpannableString
import com.tari.android.wallet.R
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.extension.applyCenterAlignment
import com.tari.android.wallet.extension.applyColorStyle
import com.tari.android.wallet.extension.applyTypefaceStyle
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import yat.android.ui.extension.HtmlHelper
import java.io.Serializable

sealed class BackupOnboardingArgs(
    val image: Int,
    val title: SpannableString,
    val description: SpannableString,
    val bottomText: SpannableString,
    val button: ButtonModule
) : Serializable {

    class StageOne(resourceManager: ResourceManager, navigationAction: () -> Unit) : BackupOnboardingArgs(
        R.drawable.vector_backup_onboarding_seed_words,
        getTitle(
            resourceManager,
            R.string.onboarding_staged_wallet_security_page1_title_part1,
            R.string.onboarding_staged_wallet_security_page1_title_part2_bold
        ),
        getBody(resourceManager, R.string.onboarding_staged_wallet_security_page1_message_full),
        getFooterMessage(resourceManager, 0),
        ButtonModule(resourceManager.getString(R.string.onboarding_staged_wallet_security_page1_action_button), ButtonStyle.Close) {
            navigationAction()
        }
    )

    class StageTwo(resourceManager: ResourceManager, navigationAction: () -> Unit) : BackupOnboardingArgs(
        R.drawable.vector_backup_onboarding_cloud,
        getTitle(
            resourceManager,
            R.string.onboarding_staged_wallet_security_page2_title_part1,
            R.string.onboarding_staged_wallet_security_page2_title_part2_bold
        ),
        getBody(resourceManager, R.string.onboarding_staged_wallet_security_page2_message_full),
        getFooterMessage(resourceManager, 1),
        ButtonModule(resourceManager.getString(R.string.onboarding_staged_wallet_security_page2_action_button), ButtonStyle.Close) {
            navigationAction()
        }
    )

    class StageThree(resourceManager: ResourceManager, navigationAction: () -> Unit) : BackupOnboardingArgs(
        R.drawable.vector_backup_onboarding_password,
        getTitle(
            resourceManager,
            R.string.onboarding_staged_wallet_security_page3_title_part1,
            R.string.onboarding_staged_wallet_security_page3_title_part2_bold
        ),
        getBody(resourceManager, R.string.onboarding_staged_wallet_security_page3_message_full),
        getFooterMessage(resourceManager, 2),
        ButtonModule(resourceManager.getString(R.string.onboarding_staged_wallet_security_page3_action_button), ButtonStyle.Close) {
            navigationAction()
        }
    )

    class StageFour(resourceManager: ResourceManager, navigationAction: () -> Unit) : BackupOnboardingArgs(
        R.drawable.vector_backup_onboarding_final,
        getTitle(
            resourceManager,
            R.string.onboarding_staged_wallet_security_page4_title_part1,
            R.string.onboarding_staged_wallet_security_page4_title_part2_bold
        ),
        getBody(resourceManager, R.string.onboarding_staged_wallet_security_page4_message_full),
        getFooterMessage(resourceManager, 3),
        ButtonModule(resourceManager.getString(R.string.onboarding_staged_wallet_security_page4_action_button), ButtonStyle.Close) {
            navigationAction()
        }
    )


    companion object {

        private val paletteManager = PaletteManager()

        private fun getTitle(resourceManager: ResourceManager, firstPartInt: Int, secondPartInt: Int): SpannableString {
            val firstPart = resourceManager.getString(firstPartInt)
            val secondPart = resourceManager.getString(secondPartInt)
            val spannable = SpannableString("$firstPart $secondPart")
            spannable.applyTypefaceStyle(secondPart, Typeface.DEFAULT_BOLD, true)
            spannable.applyCenterAlignment()
            return spannable
        }

        private fun getBody(resourceManager: ResourceManager, full: Int): SpannableString {
            val spannable = SpannableString(HtmlHelper.getSpannedText(resourceManager.getString(full)))
            spannable.applyCenterAlignment()
            return spannable
        }

        private fun getFooterMessage(resourceManager: ResourceManager, step: Int): SpannableString {
            if (step == 3) return SpannableString("")

            val firstPart = resourceManager.getString(R.string.onboarding_staged_wallet_security_footer_part1)
            val highlighted = resourceManager.getString(R.string.onboarding_staged_wallet_security_footer_part2_highlighted)
            val part3 = when (step) {
                0 -> resourceManager.getString(R.string.onboarding_staged_wallet_security_footer_part3_any_funds)
                else -> resourceManager.getString(
                    R.string.onboarding_staged_wallet_security_footer_part3_threshold,
                    StagedWalletSecurityManager.stageTwoThresholdBalance.formattedTariValue
                )
            }
            val spannable = SpannableString("$firstPart $highlighted$part3")
            spannable.applyColorStyle(highlighted, paletteManager.getTextLinks(HomeActivity.instance.get()!!))
            spannable.applyCenterAlignment()
            return spannable
        }
    }
}

