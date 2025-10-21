package com.tari.android.wallet.ui.screen.settings.allSettings.legal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.Support.ui.screen.settings.allSettings.support.LegalSettingsScreen
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class LegalSettingsFragment : CommonFragment<LegalSettingsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        TariDesignSystem(viewModel.currentTheme) {
            LegalSettingsScreen(
                onBackClick = { viewModel.onBackPressed() },
                onSettingClick = { viewModel.onSettingClick(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: LegalSettingsViewModel by viewModels()
        bindViewModel(viewModel)
    }


    companion object {
        fun newInstance() = LegalSettingsFragment()
    }
}