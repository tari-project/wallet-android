package com.tari.android.wallet.ui.dialog.modular.modules.securityStages

import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class SecurityStageHeadModule(val emojiTitle: String, val title: String, val onboardingFlowAction: () -> Unit) : IDialogModule()