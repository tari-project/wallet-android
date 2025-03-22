package com.tari.android.wallet.ui.screen.debug.sampleDesign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class SampleDesignSystemFragment : CommonFragment<SampleDesignSystemViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        TariDesignSystem(viewModel.currentTheme) {
            SampleDesignSystemScreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: SampleDesignSystemViewModel by viewModels()
        bindViewModel(viewModel)
    }
}