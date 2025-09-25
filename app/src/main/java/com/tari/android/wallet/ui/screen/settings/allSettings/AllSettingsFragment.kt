package com.tari.android.wallet.ui.screen.settings.allSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class AllSettingsFragment : CommonFragment<AllSettingsViewModel>() {

    // We need to prevent screen recording when onResume is called when this fragment is not on top
    override fun screenRecordingAlwaysDisable() = !isFragmentOnTop()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            AllSettingsScreen(
                uiState = uiState,
                onSettingClick = { viewModel.onSettingClick(it) },
                onVersionClick = { viewModel.onVersionClick() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AllSettingsViewModel by viewModels()
        bindViewModel(viewModel)
    }


    companion object {
        fun newInstance() = AllSettingsFragment()
    }
}