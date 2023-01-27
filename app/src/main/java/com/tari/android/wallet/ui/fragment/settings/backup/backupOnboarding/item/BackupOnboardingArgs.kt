package com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item

import android.text.SpannableString
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import java.io.Serializable

class BackupOnboardingArgs(val image: Int, val text: SpannableString, val args: List<ButtonModule>): Serializable

