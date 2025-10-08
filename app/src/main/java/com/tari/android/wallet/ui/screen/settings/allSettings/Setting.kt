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

    Backup(titleRes = R.string.all_settings_back_up_wallet_settings_entry, iconRes = R.drawable.vector_all_settings_backup_options_icon),
    DataCollection(titleRes = R.string.all_settings_data_collection, iconRes = R.drawable.vector_all_settings_data_collection),
    ChangePasscode(titleRes = R.string.all_settings_pin_code, iconRes = R.drawable.vector_all_settings_passcode),
    CreatePasscode(titleRes = R.string.all_settings_create_pin_code, iconRes = R.drawable.vector_all_settings_passcode),
    Biometrics(titleRes = R.string.all_settings_biometrics, iconRes = R.drawable.vector_fingerprint),
    SelectTheme(titleRes = R.string.all_settings_select_theme, iconRes = R.drawable.vector_all_settings_select_theme_icon),
    ScreenRecording(titleRes = R.string.all_settings_screen_recording, iconRes = R.drawable.vector_all_settings_screen_recording_icon),
    SelectNetwork(titleRes = R.string.all_settings_select_network, iconRes = R.drawable.vector_all_settings_select_network_icon),
    DeleteWallet(titleRes = R.string.all_settings_delete_wallet, iconRes = R.drawable.vector_all_settings_delete_button_icon),

    ReportBug(titleRes = R.string.all_settings_report_a_bug, iconRes = R.drawable.vector_all_settings_report_bug_icon),
    VisitTari(titleRes = R.string.all_settings_visit_site, iconRes = R.drawable.vector_all_settings_visit_tari_icon),
    Contribute(titleRes = R.string.all_settings_contribute, iconRes = R.drawable.vector_all_settings_contribute_to_tari_icon),
    BlockExplorer(titleRes = R.string.all_settings_explorer, iconRes = R.drawable.vector_all_settings_block_explorer_icon),
    TtlStore(titleRes = R.string.all_settings_store, iconRes = R.drawable.vector_all_settings_cart),

    About(titleRes = R.string.tari_about_title, iconRes = R.drawable.vector_all_settings_about_icon),
    UserAgreement(titleRes = R.string.all_settings_user_agreement, iconRes = R.drawable.vector_all_settings_user_agreement_icon),
    PrivacyPolicy(titleRes = R.string.all_settings_privacy_policy, iconRes = R.drawable.vector_all_settings_privacy_policy_icon),
    Disclaimer(titleRes = R.string.all_settings_disclaimer, iconRes = R.drawable.vector_all_settings_disclaimer_icon),
}