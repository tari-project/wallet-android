package com.tari.android.wallet.ui.screen.settings.allSettings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tari.android.wallet.R

enum class Setting(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Profile(titleRes = R.string.all_settings_menu_profile, iconRes = R.drawable.vector_all_settings_profile),
    Contacts(titleRes = R.string.all_settings_menu_contacts, iconRes = R.drawable.vector_all_settings_contacts),
    WalletSettings(titleRes = R.string.all_settings_menu_wallet_settings, iconRes = R.drawable.vector_all_settings_wallet_settings),
    Support(titleRes = R.string.all_settings_menu_support, iconRes = R.drawable.vector_all_settings_support_resources),
    Legal(titleRes = R.string.all_settings_menu_legal, iconRes = R.drawable.vector_all_settings_legal),
}